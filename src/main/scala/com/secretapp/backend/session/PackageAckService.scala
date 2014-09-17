package com.secretapp.backend.session

import akka.actor._
import akka.util.ByteString
import com.secretapp.backend.data.message.{MessageAck, Pong, Ping}
import com.secretapp.backend.data.transport.{MessageBox, Package}
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.common.PackageCommon.PackageToSend
import scalaz._
import Scalaz._

trait PackageAckService { this: Actor with ActorLogging =>
  import AckTrackerProtocol._

  val unackedSizeLimit = 1024 * 100
  val ackTracker = context.actorOf(Props(new AckTrackerActor(unackedSizeLimit)))

  def registerSentMessage(mb: MessageBox, b: ByteString): Unit = mb match {
    case MessageBox(mid, m) =>
      m match {
        case _: Ping =>
        case _: Pong =>
        case _: MessageAck =>
        case _ => ackTracker ! RegisterMessage(mid, b)
      }
  }

  def acknowledgeReceivedPackage(connector: ActorRef, p: Package, mb: MessageBox): Unit = mb match {
    case MessageBox(mid, m) =>
      m match {
        case _: MessageAck =>
        case _: Pong =>
        case _ =>
          // TODO: aggregation
          log.info(s"Sending acknowledgement for $m $p to $connector")

          val reply = p.replyWith(p.messageBox.messageId * 10, MessageAck(Vector(mb.messageId))).right
          connector ! reply
      }
  }
}
