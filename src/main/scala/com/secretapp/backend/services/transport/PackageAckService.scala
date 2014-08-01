package com.secretapp.backend.services.transport

import akka.actor.{Actor, Props}
import akka.util.ByteString
import com.secretapp.backend.api.{AckTrackerActor, RegisterMessage}
import com.secretapp.backend.data.message.{MessageAck, Pong, Ping}
import com.secretapp.backend.data.transport.{MessageBox, Package}
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.common.PackageCommon.PackageToSend
import scalaz._
import Scalaz._

trait PackageAckService extends PackageCommon { this: Actor =>
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

  def acknowledgeReceivedPackage(p: Package, m: MessageBox): Unit = {
    m match {
      case _ =>
        // TODO: aggregation
        log.info(s"Sending acknowledgement for $p")

        val reply = p.replyWith(m.messageId, MessageAck(Array(m.messageId))).right
        handleActor ! PackageToSend(reply)
    }
  }
}
