package com.secretapp.backend.api

import akka.actor.{ Actor, ActorRef, ActorLogging, Props }
import akka.util.{ ByteString, Timeout }
import akka.pattern.ask
import akka.io.Tcp._
import akka.event.LoggingAdapter
import com.secretapp.backend.protocol.codecs.ByteConstants
import com.secretapp.backend.persist._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.models._
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.annotation.tailrec
import scala.util.{ Success, Failure, Random }
import scodec.bits._
import scalaz._
import Scalaz._
import java.util.concurrent.ConcurrentLinkedQueue
import com.datastax.driver.core.{ Session => CSession }


trait LoggerService {
  def log : LoggingAdapter
}

trait PackageCommon extends LoggerService {
  val handleActor : ActorRef

  type PackageEither = Package \/ Package

  case class PackageToSend(p : PackageEither)
}

trait PackageAckService extends PackageCommon { this: Actor =>
  val unackedSizeLimit = 1024 * 100
  val ackTracker = context.actorOf(Props(new AckTrackerActor(unackedSizeLimit)))

  def registerSentMessage(mb : MessageBox, b : ByteString): Unit = mb match {
    case MessageBox(mid, m) =>
      m match {
        case _: Pong =>
        case _ => ackTracker ! RegisterMessage(mid, b)
      }
  }

  def acknowledgeReceivedPackage(p : Package) : Unit = {
    p.messageBox match {
      case MessageBox(_, m : Ping) =>
        log.info("Ping got, no need in acknowledgement")
      case _ =>
        // TODO: aggregation
        log.info(s"Sending acknowledgement for $p")

        val reply = p.replyWith(MessageAck(Array(p.messageBox.messageId))).right
        handleActor ! PackageToSend(reply)
    }
  }
}

trait SessionManager extends ActorLogging {
  self: Actor  =>

  import context._

  implicit val session : CSession

  private case class GetOrCreate(authId: Long, sessionId: Long)

  private val sessionManager = context.actorOf(Props(new Actor {
    var sessionFutures = new mutable.HashMap[Long, Future[Either[Long, Long]]]()

    def receive = {
      // TODO: case for forgetting sessions?
      case GetOrCreate(authId, sessionId) =>
        log.info(s"GetOrCreate $authId, $sessionId")
        val f = sessionFutures.get(sessionId) match {
          case None =>
            log.info(s"GetOrCreate Creating Future")
            val f = SessionIdRecord.getEntity(authId, sessionId).flatMap {
              case s@Some(sessionIdRecord) => Future { Left(sessionId) }
              case None =>
                SessionIdRecord.insertEntity(SessionId(authId, sessionId)).map(_ => Right(sessionId))
            }
            sessionFutures.put(sessionId, f)
            f
          case Some(f) =>
            log.info(s"GetOrCreate Returning existing Future")
            f map {
              // We already sent Right to first future processor
              case Right(sessionId) => Left(sessionId)
              case left => left
            }
        }

        val replyTo = sender()
        f map (sessionId => replyTo ! sessionId)
    }
  }))

  implicit val timeout = Timeout(5 seconds)

  /**
    * Gets existing session from database or creates new
    *
    * @param authId auth id
    * @param sessionId session id
    *
    * @return Left[Long] if existing session got or Right[Long] if new session created
    *         Perhaps we need something more convenient that Either here
    */
  protected def getOrCreateSession(authId: Long, sessionId: Long): Future[Either[Long, Long]] = {
    ask(sessionManager, GetOrCreate(authId, sessionId)).mapTo[Either[Long, Long]]
  }
}

trait PackageManagerService extends PackageCommon with SessionManager { self : Actor =>
  import context._

  implicit val session : CSession

  private var currentAuthId : Long = _
  private var currentSessionId : Long = _
  private val currentSessions = new ConcurrentLinkedQueue[Long]()
  private var currentUser : Option[User] = _

  lazy val rand = new Random()

  private def checkPackageAuth(p : Package)(f : (Package, Option[TransportMessage]) => Unit) : Unit = {
    if (p.authId == 0L) { // check for auth request - simple key registration
      if (p.sessionId == 0L) {
        val newAuthId = rand.nextLong
        AuthIdRecord.insertEntity(AuthId(newAuthId, None)).onComplete {
          case Success(_) =>
            currentAuthId = newAuthId
            f(p, None)
          case Failure(e) => sendDrop(p, e)
        }
      } else {
        sendDrop(p, s"unknown session id(${p.sessionId}) within auth id(${p.authId}})")
      }
    } else {
      AuthIdRecord.getEntity(p.authId).onComplete {
        case Success(res) => res match {
          case Some(authIdRecord) =>
            currentAuthId = authIdRecord.authId
            currentUser = authIdRecord.user
            handlePackage(p)(f)
          case None => sendDrop(p, s"unknown auth id(${p.authId}) or session id(${p.sessionId})")
        }
        case Failure(e) => sendDrop(p, e)
      }
    }
  }

  private def checkPackageSession(p : Package)(f : (Package, Option[TransportMessage]) => Unit) : Unit = {
    @inline
    def updateCurrentSession(sessionId: Long) : Unit = {
      if (currentSessionId == 0L) {
        currentSessionId = sessionId
      }
      currentSessions.add(sessionId)
    }

    if (p.authId == currentAuthId) {
      if (p.sessionId == 0L) {
        sendDrop(p, "sessionId can't be zero")
      } else {
        if (p.sessionId == currentSessionId || currentSessions.contains(p.sessionId)) {
          f(p, None)
        } else {
          getOrCreateSession(p.authId, p.sessionId).andThen {
            case Success(ms) => ms match {
              case Left(sessionId) =>
                updateCurrentSession(sessionId)
                f(p, None)
              case Right(sessionId) =>
                updateCurrentSession(sessionId)
                f(p, Some(NewSession(sessionId, p.messageBox.messageId)))
            }
            case Failure(e) => sendDrop(p, e)
          }
        }
      }
    } else {
      sendDrop(p, "you can't use two different auth id at the same connection")
    }
  }

  final def handlePackage(p : Package)(f : (Package, Option[TransportMessage]) => Unit) : Unit = {
    if (currentAuthId == 0L) { // check for empty auth id - it mean a new connection
      checkPackageAuth(p)(f)
    } else {
      checkPackageSession(p)(f)
    }
  }

  def getAuthId = currentAuthId

  def getSessionId = currentSessionId

  private def sendDrop(p : Package, msg : String) : Unit = {
    val reply = p.replyWith(Drop(_, msg)).left
    handleActor ! PackageToSend(reply)
  }

  private def sendDrop(p : Package, e : Throwable) : Unit = sendDrop(p, e.getMessage)
}

sealed trait HandleError
case class ParseError(msg : String) extends HandleError // error which caused when package parsing (we can't parse authId/sessionId/messageId)

trait WrappedPackageService extends PackageManagerService with PackageAckService { self : Actor =>

  import ByteConstants._

  sealed trait ParseState
  case class WrappedPackageSizeParsing() extends ParseState
  case class WrappedPackageParsing(bitsLen : Long) extends ParseState

  type ParseResult = (ParseState, BitVector)
  type PackageFunc = (Package, Option[TransportMessage]) => Unit

  val minParseLength = varint.maxSize * byteSize // we need first 10 bytes for package size: package size varint (package + crc) + package + crc 32 int 32
  val maxPackageLen = (1024 * 1024 * 1.5).toLong // 1.5 MB

  @tailrec
  private final def parseByteStream(state : ParseState, buf : BitVector)(f : PackageFunc) : HandleError \/ ParseResult =
    state match {
      case sp@WrappedPackageSizeParsing() =>
        if (buf.length >= minParseLength) {
          varint.decode(buf) match {
            case \/-((_, len)) =>
              val pLen = (len + varint.sizeOf(len)) * byteSize // length of Package payload (with crc) + length of varint before Package
              if (len <= maxPackageLen) {
                parseByteStream(WrappedPackageParsing(pLen), buf)(f)
              } else {
                ParseError(s"received package size $len is bigger than $maxPackageLen bytes").left
              }
            case -\/(e) => ParseError(e).left
          }
        } else {
          (sp, buf).right
        }

      case pp@WrappedPackageParsing(bitsLen) =>
        if (buf.length >= bitsLen) {
          protoPackageBox.decode(buf) match {
            case \/-((remain, wp)) =>
              handlePackage(wp.p)(f)
              log.info(s"remain: $remain, buf: $buf")
              parseByteStream(WrappedPackageSizeParsing(), remain)(f)
            case -\/(e) => ParseError(e).left
          }
        } else {
          (pp, buf).right
        }

      case _ => ParseError("internal error: wrong state").left
    }


  private var parseState : ParseState = WrappedPackageSizeParsing()
  private var parseBuffer = BitVector.empty

  /**
   * Parse bit stream, handle Package's and parsing failures.
   *
   * @param buf bit stream for parsing and handling
   * @param packageFunc handle Package function and maybe additional reply message
   * @param failureFunc handle parsing failures function
   */
  final def handleByteStream(buf : BitVector)(packageFunc : PackageFunc, failureFunc : (HandleError) => Unit) : Unit = {
    parseByteStream(parseState, parseBuffer ++ buf)(packageFunc) match {
      case \/-((newState, remainBuf)) =>
        parseState = newState
        parseBuffer = remainBuf
      case -\/(e) =>
        log.error(s"handleByteStream#$parseState: $e")
        failureFunc(e)
    }
  }


  def replyPackage(p : Package) : ByteString = {
    protoPackageBox.encode(p) match {
      case \/-(bv) =>
        val bs = ByteString(bv.toByteArray)
        registerSentMessage(p.messageBox, bs)
        bs
      case -\/(e) => ByteString(e)
    }
  }

}

trait PackageHandler extends PackageManagerService with PackageAckService {  self : Actor =>

  def handlePackage(p : Package, pMsg : Option[TransportMessage]) : Unit = {
    pMsg match {
      case Some(m) =>
        log.info(s"m: $m")
        handleActor ! PackageToSend(p.replyWith(m).right)
      case None =>
    }

    if (p.authId == 0L && p.sessionId == 0L) {
      val reply = p.replyWith(ResponseAuthId(getAuthId)).right
      handleActor ! PackageToSend(reply)
    } else {
      acknowledgeReceivedPackage(p)
      p.messageBox.body match { // TODO: move into pluggable traits
        case Ping(randomId) =>
          val reply = p.replyWith(Pong(randomId)).right
          handleActor ! PackageToSend(reply)
        //        case RpcRequest(rpcMessage) =>
        //          rpcMessage match {
        //            case SendSMSCode(phoneNumber, _, _) =>
        //
        //            case SignUp(phoneNumber, smsCodeHash, smsCode, _, _, _, _) =>
        //            case SignIn(phoneNumber, smsCodeHash, smsCode) =>
        //          }
        //
        //          s"rpc message#$rpcMessage is not implemented yet".left
        //        case _ => s"unknown case for message".left
        case MessageAck(mids) =>
          ackTracker ! RegisterMessageAcks(mids.toList)
        case _ =>
      }
    }
  }

  def handleError(e : HandleError) : Unit = e match {
    case ParseError(msg) =>
      val reply = Package(0L, 0L, MessageBox(0L, Drop(0L, msg))).left
      handleActor ! PackageToSend(reply)
    case _ => log.error("unknown handle error")
  }

}
