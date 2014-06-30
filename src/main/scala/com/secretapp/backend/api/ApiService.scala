package com.secretapp.backend.api

import akka.actor.{ ActorRef, ActorLogging }
import scala.annotation.tailrec
import scodec.bits._
import java.util.concurrent.ConcurrentHashMap
import scalaz._
import Scalaz._

trait ApiService {

  val authTable: ConcurrentHashMap[Long, Long]

  import com.secretapp.backend.protocol._

  sealed trait ParseState
  case class PackageParsing() extends ParseState
  case class MessageParsing(p: PackageHead) extends ParseState
  case class DropParsing(e: String) extends ParseState

  type ParseResult = (ParseState, BitVector)
  var state: ParseResult = (PackageParsing(), BitVector.empty)
  def dropState(e: String) = (DropParsing(e), BitVector.empty)

  type HandleResult[T] = Either[Throwable, T]

  //  TODO: check for max buffer length and drop parsing for GC
  @tailrec
  final def handleReceivedBytes(s: ParseState, buf: BitVector)(con: ActorRef): ParseResult = s match {
    case PackageParsing() =>
      if (buf.length >= Package.headerBitSize) {
        parsePackage(buf.take(Package.headerBitSize)) match {
          case Right(p) => handleReceivedBytes(MessageParsing(p), buf.drop(Package.headerBitSize))(con)
          case Left(e) => dropState(e.getMessage)
        }
      } else (PackageParsing(), buf)

    case MessageParsing(p) =>
      if (buf.length >= p.messageBitLength) {
        parseMessage(buf.take(p.messageBitLength)) match {
          case Right(m) =>
            handleMessage(Package(p, m))(con) match {
              case Right(_) => handleReceivedBytes(PackageParsing(), buf.drop(p.messageBitLength))(con)
              case Left(e) => dropState(e.getMessage)
            }

          case Left(e) => dropState(e.getMessage)
        }
      } else (MessageParsing(p), buf)

    case _ => dropState("unknown state")
  }

  def parsePackage(buf: BitVector): HandleResult[PackageHead] =
    Package.codecHead.decode(buf) match {
      case \/-((_, p)) =>
        // TODO: validate p
        Right(p)

      case -\/(e) => Left(new Throwable(e))
    }

  def parseMessage[T <: PackageMessage[_]](buf: BitVector): HandleResult[T] = {
    Left(new Throwable("parse error"))
  }

  var messageCounter: Long = 0L

  def handleMessage(p: Package[_])(con: ActorRef): HandleResult[Unit] = {
    Right(())
  }

  var authId: Option[Long] = None

}
