package com.secretapp.backend.api

import akka.actor.{ ActorRef, ActorLogging }
import akka.util.ByteString
import akka.io.Tcp._
import akka.event.LoggingAdapter
import com.secretapp.backend.protocol.codecs.ByteConstants
import com.secretapp.backend.persist._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data._
import scala.annotation.tailrec
import scala.util.Random
import scodec.bits._
import scalaz._
import Scalaz._
import com.secretapp.backend.data
import java.util.concurrent.{ ConcurrentHashMap, ConcurrentSkipListSet }


trait LoggerService {

  def log : LoggingAdapter

}


trait WrappedPackageService {

  import ByteConstants._

  sealed trait ParseState
  case class WrappedPackageSizeParsing() extends ParseState
  case class WrappedPackageParsing(bitsLen : Long) extends ParseState

  type ParseResult = (ParseState, BitVector)

  val minParseLength = varint.maxSize * byteSize // we need first 10 bytes for package size: package size varint (package + crc) + package + crc 32 int 32
  val maxPackageLen = (1024 * 1024 * 1.5).toLong // 1.5 MB

  sealed trait HandleError
  case class ParseError(msg : String) extends HandleError // error which caused when package parsing (we can't parse authId/sessionId/messageId)

  @tailrec
  final def handleByteStream(state : ParseState, buf : BitVector)(f : (Package) => Unit) : HandleError \/ ParseResult =
    state match {
      case sp@WrappedPackageSizeParsing() =>
        if (buf.length >= minParseLength) {
          varint.decode(buf) match {
            case \/-((_, len)) =>
              val pLen = (len + varint.sizeOf(len)) * byteSize // length of Package payload (with crc) + length of varint before Package
              if (len <= maxPackageLen) {
                handleByteStream(WrappedPackageParsing(pLen), buf)(f)
              } else {
                ParseError(s"received package size $len is bigger than $maxPackageLen bytes").left
              }
            case -\/(e) => ParseError(e).left
          }
        } else (sp, buf).right

      case pp@WrappedPackageParsing(bitsLen) =>
        if (buf.length >= bitsLen) {
          protoWrappedPackage.decode(buf) match {
            case \/-((remain, wp)) =>
              f(wp.p)
              (WrappedPackageSizeParsing(), remain).right
            case -\/(e) => ParseError(e).left
          }
        } else (pp, buf).right

      case _ => ParseError("internal error: wrong state").left
    }

}


trait ApiService {

  val authTable: ConcurrentHashMap[Long, ConcurrentSkipListSet[Long]]

  import com.secretapp.backend.protocol.codecs.ByteConstants._

  sealed trait ParseState
  case class WrappedPackageSizeParsing() extends ParseState
  case class WrappedPackageParsing(bitsLen: Long) extends ParseState
  case class DropParsing(authId: Long, sessionId: Long, messageId: Long, e: String) extends ParseState

  type ParseResult = (ParseState, BitVector)
  var state: ParseResult = (WrappedPackageSizeParsing(), BitVector.empty)
  var sendBuffer: ByteString = ByteString()

  def dropState(authId: Long, sessionId: Long, messageId: Long, e: String) = {
    (DropParsing(authId, sessionId, messageId, e), BitVector.empty)
  }

  type HandleResult = String \/ Unit

  val minParseLength = varint.maxSize * byteSize // we need first 10 bytes for package size: package size varint (package + crc) + package + crc 32 int 32

  @tailrec
  final def handleStream(s: ParseState, buf: BitVector): ParseResult = s match {
    case wpsp@WrappedPackageSizeParsing() =>
      if (buf.length >= minParseLength) {
        varint.decode(buf) match {
          case \/-((_, len)) =>
            val pLen = len * byteSize + varint.sizeOf(len) * byteSize
            handleStream(WrappedPackageParsing(pLen), buf)
          case -\/(e) => dropState(authId.getOrElse(0), sessionId.getOrElse(0), 0L, e)
        }
      } else (wpsp, buf)

    case wpp@WrappedPackageParsing(bitsLen) =>
      if (buf.length >= bitsLen) {
        protoWrappedPackage.decode(buf) match {
          case \/-((remain, wp)) =>
            val handleRes = for {
              _ <- validatePackage(wp.p)
              _ <- handleMessage(wp.p)
            } yield ()

            handleRes match {
              case \/-(_) => (WrappedPackageSizeParsing(), remain)
              case -\/(e) => dropState(wp.p.authId, wp.p.sessionId, wp.p.message.messageId, e)
            }
          case -\/(e) => dropState(authId.getOrElse(0), sessionId.getOrElse(0), 0L, e)
        }
      } else (wpp, buf)

    case _ => dropState(authId.getOrElse(0), sessionId.getOrElse(0), 0L, "Internal error: wrong state.")
  }

  def validatePackage(p: Package): HandleResult = {
    if (Some(p.authId) != authId && p.authId != 0L) {
      if (authTable.containsKey(p.authId)) {
        authId = Some(p.authId)
      } else {
        return s"unknown authId($authId)".left
      }
    }

    if (Some(p.sessionId) != sessionId && !sessionIds.contains(p.sessionId) && p.sessionId != 0L) {
      val sessions = authTable.get(p.authId)
      if (sessions == null) {
        return s"authId(${p.authId}) not found".left
      }

      if (sessions.contains(p.sessionId)) {
        sessionIds = sessionIds :+ p.sessionId
      } else {
        sessionId = Some(p.sessionId)
        sessionIds = sessionIds.+:(p.sessionId)
        sessions.add(p.sessionId)
        val wrappedMsg = ProtoMessageWrapper(p.message.messageId, NewSession(p.sessionId, p.message.messageId))
        writeCodecResult(p.authId, p.sessionId, wrappedMsg)
      }
    }

    ().right
  }

  def writeCodecResult(authId: Long, sessionId: Long, m: ProtoMessageWrapper): HandleResult = {
    protoWrappedPackage.encode(Package(authId, sessionId, m)) match {
      case \/-(b) =>
        sendBuffer ++= ByteString(b.toByteBuffer)
        ().right
      case -\/(e) => e.left
    }
  }

  def handleMessage(p: Package): HandleResult = authId match {
    case Some(authId) =>
      p.message.body match {
        case Ping(randomId) =>
          val wrappedMsg = ProtoMessageWrapper(p.message.messageId, Pong(randomId))
          writeCodecResult(p.authId, p.sessionId, wrappedMsg)
        case RpcRequest(rpcMessage) =>
          rpcMessage match {
            case SendSMSCode(phoneNumber, _, _) =>

            case SignUp(phoneNumber, smsCodeHash, smsCode, _, _, _, _) =>
            case SignIn(phoneNumber, smsCodeHash, smsCode) =>
          }

          s"rpc message#$rpcMessage is not implemented yet".left
        case _ => s"unknown case for message".left
      }

    case None =>
      p.message.body match {
        case RequestAuthId() if p.authId == 0L && p.sessionId == 0L =>
          val newAuthId = rand.nextLong
          authId = Some(newAuthId)
//          AuthIdRecord.insertEntity(AuthId(newAuthId, None))(DBConnector.session)
          authTable.put(newAuthId, new ConcurrentSkipListSet[Long]()) // TODO: check for uniqueness
          val wrappedMsg = ProtoMessageWrapper(p.message.messageId, ResponseAuthId(newAuthId))
          writeCodecResult(p.authId, p.sessionId, wrappedMsg)
        case _ => s"unknown authId(${p.authId}) or sessionId(${p.sessionId})".left
      }
  }

  var authId: Option[Long] = None
  var sessionId: Option[Long] = None
  var sessionIds = Vector[Long]()
  lazy val rand = new Random()

}
