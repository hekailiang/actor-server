package com.secretapp.backend.api

import akka.actor.{ Actor, ActorRef, ActorLogging, Props }
import akka.util.ByteString
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


trait PackageManagerService extends PackageCommon { self : Actor =>

  import context._

  implicit val session : CSession

  private var currentAuthId : Long = _
  private var currentSessionId : Long = _
  private val currentSessions = new ConcurrentLinkedQueue[Long]()
  private var currentUser : Option[User] = _

  lazy val rand = new Random()

  final def handlePackage(p : Package)(f : (Package, Option[TransportMessage]) => Unit) : Unit = {
    @inline
    def updateCurrentSession() : Unit = {
      if (currentSessionId == 0L) {
        currentSessionId = p.sessionId
      }
      currentSessions.add(p.sessionId)
    }

    if (currentAuthId == 0L) { // check for empty auth id - it mean a new connection

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

    } else {

      if (p.authId == currentAuthId) {
        if (p.sessionId == 0L) {
          sendDrop(p, "sessionId can't be zero")
        } else {
          if (p.sessionId == currentSessionId || currentSessions.contains(p.sessionId)) {
            f(p, None)
          } else {
            SessionIdRecord.getEntity(p.authId, p.sessionId).onComplete {
              case Success(res) => res match {
                case Some(sessionIdRecord) =>
                  updateCurrentSession()
                  f(p, None)
                case None =>
                  SessionIdRecord.insertEntity(SessionId(p.authId, p.sessionId)).onComplete {
                    case Success(_) =>
                      updateCurrentSession()
                      f(p, Some(NewSession(p.sessionId, p.messageBox.messageId)))
                    case Failure(e) => sendDrop(p, e)
                  }
              }
              case Failure(e) => sendDrop(p, e)
            }
          }
        }
      } else {
        sendDrop(p, "you can't use two different auth id at the same connection")
      }

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
              (WrappedPackageSizeParsing(), remain).right
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
      case Some(m) => handleActor ! PackageToSend(p.replyWith(m).right)
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
