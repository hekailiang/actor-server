package com.secretapp.backend.session

import akka.actor._
import akka.event.LoggingAdapter
import akka.util.Timeout
import akka.pattern.ask
import com.secretapp.backend.data.message.{ MessageAck, Pong, Ping }
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.api.frontend._
import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.Future
import scalaz._
import Scalaz._
import scodec.bits.BitVector

trait PackageAckService { self: MessageIdGenerator with TransportSerializers =>
  import AckTrackerProtocol._

  val authId: Long
  val sessionId: Long
  val context: ActorContext
  var transport: Option[TransportConnection]
  def log: LoggingAdapter

  import context.dispatcher

  // TODO: configurable
  val unackedSizeLimit = 1024 * 100
  lazy val ackTracker = context.actorOf(Props(classOf[AckTrackerActor], authId, sessionId, unackedSizeLimit))

  def registerSentMessage(mb: MessageBox, b: BitVector): Unit = mb match {
    case MessageBox(mid, m) =>
      m match {
        case _: Ping | _: Pong | _: MessageAck =>
        case _ =>
          log.debug(s"Registering sent message $mb")
          ackTracker ! RegisterMessage(mid, b)
      }
  }

  def acknowledgeReceivedPackage(connector: ActorRef, mb: MessageBox): Unit = mb match {
    case MessageBox(mid, m) =>
      m match {
        case _: MessageAck | _: Pong =>
        case _ =>
          // TODO: aggregation
          log.info(s"Sending acknowledgement for $m to $connector")

          val reply = serializePackage(MessageBox(getMessageId(TransportMsgId), MessageAck(Vector(mb.messageId))))
          connector ! reply
      }
  }

  def getUnsentMessages(): Future[immutable.Map[Long, BitVector]] = {
    implicit val timeout = Timeout(5.seconds)
    ask(ackTracker, GetUnackdMessages).mapTo[UnackdMessages] map (_.messages)
  }
}
