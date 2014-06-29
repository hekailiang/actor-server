package com.secretapp.backend

import scala.annotation.tailrec
import akka.actor.{ Actor, ActorRef, ActorLogging, Props }
import akka.io.{ IO, Tcp }
import akka.util.ByteString
import Tcp._

class Server extends Actor with ActorLogging {

  import context.system

  def receive = {
    case b @ Bound(localAddress) =>
      log.info(s"Bound: $b")
    case CommandFailed(_: Bind) =>
      log.info("CommandFailed")
      context stop self
    case c @ Connected(remote, local) =>
      log.info(s"Connected: $c")
      val handler = context.actorOf(Props[ApiHandler])
      val connection = sender()
      connection ! Register(handler)
  }
}


class ApiHandler extends Actor with ActorLogging {

  import scodec.bits._
  import protocol._

  sealed trait ParseState
  case class PackageParsing() extends ParseState
  case class MessageParsing(p: PackageHead) extends ParseState
  case class DropParsing(e: Option[Throwable] = None) extends ParseState

  type ParseResult = (ParseState, BitVector)
  var state: ParseResult = (PackageParsing(), BitVector.empty)
  val dropState = (DropParsing(), BitVector.empty)

  @tailrec
  private def handleReceivedBytes(s: ParseState, buf: BitVector): ParseResult = s match {
    case PackageParsing() =>
      if (buf.length >= Package.headerBitSize) {
        parsePackage(buf.take(Package.headerBitSize)) match {
          case Some(p) =>
            handleReceivedBytes(MessageParsing(p), buf.drop(Package.headerBitSize))
          case None => dropState
        }
      } else (PackageParsing(), buf)

    case MessageParsing(p) =>
      if (buf.length >= p.messageBitLength) {
        parseMessage(buf.take(p.messageBitLength)) match {
          case Some(m) =>
            handleMessage(Package(p, m))
            handleReceivedBytes(PackageParsing(), buf.drop(p.messageBitLength))
          case None => dropState
        }
      } else (MessageParsing(p), buf)

    case _ => dropState
  }

  def parsePackage(buf: BitVector): Option[PackageHead] =
    Package.codecHead.decode(buf).toOption match {
      case Some((_, p)) =>
        // TODO: validate p
        Some(p)
      case None => None
    }

  def parseMessage[T <: PackageMessage[_]](buf: BitVector): Option[T] = {
    None
  }

  var messageCounter: Long = 0L

  def handleMessage(p: Package[_]) = {

  }

  def receive = {
    case Received(data) =>
      val connection = sender()
      log.info(s"Received: $data ${data.length}")

      state = handleReceivedBytes(state._1, state._2 ++ BitVector(data.toArray))
      log.info(s"state: $state")
      state._1 match {
        case DropParsing(e) =>
          log.info(s"DropParsing: $e}")
//          val dropMsg = Struct.dropCodec.encode(e.getMessage)
//          connection ! Write(dropMsg)
          connection ! Close
          context stop self
        case _ =>
      }

    case PeerClosed =>
      log.info("PeerClosed")
      context stop self
  }
}
