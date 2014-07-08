package com.secretapp.backend.api

import akka.actor.{ ActorRef, ActorLogging }
import akka.util.ByteString
import akka.io.Tcp._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data._
import scala.annotation.tailrec
import scala.util.Random
import scodec.bits._
import scalaz._
import Scalaz._
import com.secretapp.backend.data
import java.util.concurrent.{ ConcurrentHashMap, ConcurrentSkipListSet }

trait ApiService {

  val authTable: ConcurrentHashMap[Long, ConcurrentSkipListSet[Long]]

  import ByteConstants._

  sealed trait ParseState
  case class WrappedPackageSizeParsing() extends ParseState
  case class WrappedPackageParsing(bitsLen: Long) extends ParseState
  case class DropParsing(e: String) extends ParseState

  type ParseResult = (ParseState, BitVector)
  var state: ParseResult = (WrappedPackageSizeParsing(), BitVector.empty)
  var sendBuffer: ByteString = ByteString()
  def dropState(e: String) = (DropParsing(e), BitVector.empty)

  type HandleResult = String \/ Unit

  val minParseLength = 6 * byteSize // we need first 6 bytes for package size: package size varint (package + crc) + package + crc 32 int 32

  @tailrec
  final def handleStream(s: ParseState, buf: BitVector): ParseResult = s match {
    case wpsp@WrappedPackageSizeParsing() =>
      if (buf.length >= minParseLength) {
        VarIntCodec.decode(buf) match {
          case \/-((_, len)) =>
            val pLen = len * byteSize + VarIntCodec.sizeOf(len) * byteSize
            handleStream(WrappedPackageParsing(pLen), buf)
          case -\/(e) => dropState(e)
        }
      } else (wpsp, buf)

    case wpp@WrappedPackageParsing(bitsLen) =>
      if (buf.length >= bitsLen) {
        WrappedPackageCodec.decode(buf) match {
          case \/-((remain, wp)) =>
            val handleRes = for {
              _ <- validatePackage(wp.p)
              _ <- handleMessage(wp.p)
            } yield ()

            handleRes match {
              case \/-(_) => (WrappedPackageSizeParsing(), remain)
              case -\/(e) => dropState(e)
            }
          case -\/(e) => dropState(e)
        }
      } else (wpp, buf)

    case _ => dropState("Internal error: wrong state.")
  }

  def validatePackage(p: Package): HandleResult = {
    if (Some(p.authId) != authId && p.authId != 0L) {
      if (authTable.containsKey(p.authId)) authId = Some(p.authId)
      else return s"unknown authId($authId)".left
    }

    if (Some(p.sessionId) != sessionId && !sessionIds.contains(p.sessionId) && p.sessionId != 0L) {
      val sessions = authTable.get(p.authId)
      if (sessions == null) return s"empty authTable".left

      if (sessions.contains(p.sessionId)) {
        sessionIds = sessionIds :+ p.sessionId
      } else {
        sessionId = Some(p.sessionId)
        sessionIds = sessionIds.+:(p.sessionId)
        sessions.add(p.sessionId)
        writeCodecResult(p.authId, p.sessionId, MessageWrapper(p.message.messageId, NewSession(p.sessionId, p.message.messageId)))
      }
    }

    ().right
  }

  def writeCodecResult(authId: Long, sessionId: Long, m: MessageWrapper): HandleResult = {
    packageCodec.encode(Package(authId, sessionId, m)) match {
      case \/-(b) =>
        sendBuffer ++= ByteString(b.toByteBuffer)
        ().right
      case -\/(e) => e.left
    }
  }

  def handleMessage(p: Package): HandleResult = authId match {
    case Some(authId) =>
      p.message.body match {
        case Ping(randomId) => writeCodecResult(p.authId, p.sessionId, MessageWrapper(p.message.messageId, Pong(randomId)))
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
          authTable.put(newAuthId, new ConcurrentSkipListSet[Long]()) // TODO: check for uniqueness
          writeCodecResult(p.authId, p.sessionId, MessageWrapper(p.message.messageId, ResponseAuthId(newAuthId)))
        case _ => s"unknown authId(${p.authId}) or sessionId(${p.sessionId})".left
      }
  }

  var authId: Option[Long] = None
  var sessionId: Option[Long] = None
  var sessionIds = Vector[Long]()
  lazy val rand = new Random()

}
