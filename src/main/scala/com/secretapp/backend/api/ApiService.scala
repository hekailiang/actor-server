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
  case class PackageParsing() extends ParseState
  case class MessageParsing(p: PackageHead) extends ParseState
  case class DropParsing(e: String) extends ParseState

  type ParseResult = (ParseState, BitVector)
  var state: ParseResult = (PackageParsing(), BitVector.empty)
  var sendBuffer: ByteString = ByteString()
  def dropState(e: String) = (DropParsing(e), BitVector.empty)

  type HandleResult[T] = Either[String, T]

  //  TODO: replace with scalaz State monad
  //  TODO: check for max buffer length and drop parsing for GC
  @tailrec
  final def handleReceivedBytes(s: ParseState, buf: BitVector): ParseResult = s match {
    case PackageParsing() =>
      if (buf.length >= Package.headerBitSize) {
        println(s"PackageParsing(): buf.take ${Package.headerBitSize} ${buf.length}")
        parsePackage(buf.take(Package.headerBitSize)) match {
          case Right(p) =>
            val newState = if (p.messageBitLength > 0) MessageParsing(p) else PackageParsing()
            handleReceivedBytes(newState, buf.drop(Package.headerBitSize))
          case Left(e) => dropState(e)
        }
      } else (PackageParsing(), buf)

    case MessageParsing(p) =>
      if (buf.length >= p.messageBitLength) {
        println(s"MessageParsing(): buf.take ${p.messageBitLength} ${buf.length}")
        parseMessage(buf.take(p.messageBitLength)) match {
          case Right(m) =>
            handleMessage(p, m) match {
              case Right(_) => handleReceivedBytes(PackageParsing(), buf.drop(p.messageBitLength))
              case Left(e) => dropState(e)
            }

          case Left(e) => dropState(e)
        }
      } else (MessageParsing(p), buf)

    case _ => dropState("unknown state")
  }

  def parsePackage(buf: BitVector): HandleResult[PackageHead] = {
    Package.codecHead.decode(buf) match {
      case \/-((_, p)) =>
        if (Some(p.authId) != authId && p.authId != 0L) {
          if (authTable.containsKey(p.authId)) authId = Some(p.authId)
          else return Left(s"unknown authId($authId)")
        }

        if (Some(p.sessionId) != sessionId && !sessionIds.contains(p.sessionId) && p.sessionId != 0L) {
          val sessions = authTable.get(p.authId)
          if (sessions != null) return Left(s"empty authTable")

          if (sessions.contains(p.sessionId)) {
            sessionIds = sessionIds :+ p.sessionId
          } else {
            val newSessionId = rand.nextLong
            sessionId = Some(newSessionId)
            sessionIds = sessionIds.+:(newSessionId)
            sessions.add(newSessionId)
            writeCodecResult(p, NewSession(newSessionId, p.messageId))
          }
        }

        Right(p)
      case -\/(e) => Left(e)
    }
  }

  def parseMessage(buf: BitVector): HandleResult[PackageMessage] = {
    Message.codec.decode(buf) match {
      case \/-((_, m)) =>
        println(PackageMessage(m))
        Right(PackageMessage(m))
      case -\/(e) => Left(e)
    }
  }

  def writeCodecResult(p: PackageHead, m: codecs.Message): HandleResult[Unit] = {
    val res = for {
      reply <- Message.codec.encode(m)
      packageHead = PackageHead(p.authId, p.sessionId, p.messageId, (reply.length / 8).toInt)
      head <- Package.codecHead.encode(packageHead)
    } yield ByteString(head.toByteBuffer) ++ ByteString(reply.toByteBuffer)
    res match {
      case \/-(b) =>
        sendBuffer ++= b
        Right(())
      case -\/(e) => Left(e)
    }
  }

  def handleMessage(head: PackageHead, msg: PackageMessage): HandleResult[Unit] = authId match {
    case Some(authId) =>
      println(head)
      println(msg)

      msg.message match {
        case Ping(randomId) =>
          println(s"Ping: $randomId")
          writeCodecResult(head, Pong(randomId))
        case _ =>
          Left(s"unknown case for message")
      }

    case None =>
      msg.message match {
        case RequestAuthId() if head.authId == 0L && head.sessionId == 0L =>
          val newAuthId = rand.nextLong
          authId = Some(newAuthId)
          authTable.put(newAuthId, new ConcurrentSkipListSet[Long]()) // TODO: check for uniqueness
          writeCodecResult(head, ResponseAuthId(newAuthId))
        case _ =>
          Left(s"unknown authId(${head.authId}) or sessionId(${head.sessionId})")
      }
  }

  var authId: Option[Long] = None
  var sessionId: Option[Long] = None
  var sessionIds: Seq[Long] = Seq[Long]()
  lazy val rand = new Random()

}
