package com.secretapp.backend.session

import akka.actor._
import akka.event.LoggingAdapter
import akka.util.{ ByteString, Timeout }
import akka.pattern.ask
import com.secretapp.backend.data.message.{ MessageAck, Pong, Ping }
import com.secretapp.backend.data.transport.{ MessageBox, MTPackage }
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.common.PackageCommon.PackageToSend
import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.Future
import scalaz._
import Scalaz._

trait PackageAckService {
  import AckTrackerProtocol._

  val authId: Long
  val sessionId: Long
  val context: ActorContext
  def log: LoggingAdapter

  import context.dispatcher

  // TODO: configurable
  val unackedSizeLimit = 1024 * 1024 * 16 // 16 MB
  lazy val ackTracker = context.actorOf(Props(classOf[AckTrackerActor], authId, sessionId, unackedSizeLimit))

  def registerSentMessage(mb: MessageBox, b: ByteString): Unit = mb match {
    case MessageBox(mid, m) =>
      m match {
        case _: Ping =>
        case _: Pong =>
        case _: MessageAck =>
        case _ =>
          log.debug(s"Registering sent message $mb")
          ackTracker ! RegisterMessage(mid, b)
      }
  }

  def acknowledgeReceivedPackage(connector: ActorRef, mb: MessageBox): Unit = mb match {
    case MessageBox(mid, m) =>
      m match {
        case _: MessageAck =>
        case _: Pong =>
        case _ =>
          // TODO: aggregation
          log.info(s"Sending acknowledgement for $m to $connector")

          val reply = mb.replyWith(authId, sessionId, mb.messageId * 10, MessageAck(Vector(mb.messageId))).right
          connector ! reply
      }
  }

  def getUnsentMessages(): Future[immutable.Map[Long, ByteString]] = {
    implicit val timeout = Timeout(5.seconds)
    ask(ackTracker, GetUnackdMessages).mapTo[UnackdMessages] map (_.messages)
  }
}
