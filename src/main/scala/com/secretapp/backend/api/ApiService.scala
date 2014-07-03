package com.secretapp.backend.api

import akka.actor.{ ActorRef, ActorLogging }
import akka.util.ByteString
import akka.io.Tcp._
import com.secretapp.backend.protocol.codecs._
import scala.annotation.tailrec
import scala.util.Random
import scodec.bits._
import java.util.concurrent.{ ConcurrentHashMap, ConcurrentSkipListSet }
import scalaz._
import Scalaz._

trait ApiService {

  val authTable: ConcurrentHashMap[Long, ConcurrentSkipListSet[Long]]

  import com.secretapp.backend.protocol._

  sealed trait ParseState
  case class LengthParsing() extends ParseState
  case class PackageParsing(bitsLen: Long) extends ParseState
  case class DropParsing(e: String) extends ParseState

  type ParseResult = (ParseState, BitVector)
  var state: ParseResult = (LengthParsing(), BitVector.empty)
  var sendBuffer: ByteString = ByteString()
  def dropState(e: String) = (DropParsing(e), BitVector.empty)

  type HandleResult = String \/ Unit

  val minParseLength = 40L

  @tailrec
  final def handleReceivedBytes(s: ParseState, buf: BitVector): ParseResult = s match {
    case lp@LengthParsing() =>
      if (buf.length >= minParseLength) {
        VarInt.decode(buf) match {
          case \/-((_, len)) =>
            val pLen = len * 8 + VarInt.sizeOf(len) * 8
            handleReceivedBytes(PackageParsing(pLen), buf)
          case -\/(e) => dropState(e)
        }
      } else (lp, buf)

    case pp@PackageParsing(bitsLen) =>
      if (buf.length >= bitsLen) {
        Package.decode(buf) match {
          case \/-((remain, p)) =>
            val res = for {
              _ <- validatePackage(p)
              _ <- handleMessage(p)
            } yield (LengthParsing(), remain)
            res match {
              case \/-(r@((_, _))) => r
              case -\/(e) => dropState(e)
            }
          case -\/(e) => dropState(e)
        }
      } else (pp, buf)

    case _ => dropState("unknown state")
  }

  def validatePackage(p: Package): HandleResult = {
    val ph = p.head
    if (Some(ph.authId) != authId && ph.authId != 0L) {
      if (authTable.containsKey(ph.authId)) authId = Some(ph.authId)
      else return s"unknown authId($authId)".left
    }

    if (Some(ph.sessionId) != sessionId && !sessionIds.contains(ph.sessionId) && ph.sessionId != 0L) {
      val sessions = authTable.get(ph.authId)
      if (sessions != null) return s"empty authTable".left

      if (sessions.contains(ph.sessionId)) {
        sessionIds = sessionIds :+ ph.sessionId
      } else {
        val newSessionId = rand.nextLong
        sessionId = Some(newSessionId)
        sessionIds = sessionIds.+:(newSessionId)
        sessions.add(newSessionId)
        writeCodecResult(ph, NewSession(newSessionId, ph.messageId))
      }
    }

    ().right
  }

  def writeCodecResult(p: PackageHead, m: codecs.Message): HandleResult = {
    Package.encode(p.authId, p.sessionId, p.messageId, m) match {
      case \/-(b) =>
        sendBuffer ++= ByteString(b.toByteBuffer)
        ().right
      case -\/(e) => e.left
    }
  }

  def handleMessage(p: Package): HandleResult = authId match {
    case Some(authId) =>
      p.message match {
        case Ping(randomId) => writeCodecResult(p.head, Pong(randomId))
        case RpcRequest(rpcMessageId, rpcMessage) =>
          s"rpc message $rpcMessage is not implemented yet".left
        case _ => s"unknown case for message".left
      }

    case None =>
      p.message match {
        case RequestAuthId() if p.head.authId == 0L && p.head.sessionId == 0L =>
          val newAuthId = rand.nextLong
          authId = Some(newAuthId)
          authTable.put(newAuthId, new ConcurrentSkipListSet[Long]()) // TODO: check for uniqueness
          writeCodecResult(p.head, ResponseAuthId(newAuthId))
        case _ => s"unknown authId(${p.head.authId}) or sessionId(${p.head.sessionId})".left
      }
  }

  var authId: Option[Long] = None
  var sessionId: Option[Long] = None
  var sessionIds: Seq[Long] = Seq[Long]()
  lazy val rand = new Random()

}
