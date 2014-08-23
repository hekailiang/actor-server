package com.secretapp.backend.services.transport

import akka.actor.Actor
import com.secretapp.backend.api.ApiHandlerActor
import com.secretapp.backend.api.RegisterMessageAcks
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.transport.{MessageBox, Package}
import com.secretapp.backend.services.RpcService
import com.secretapp.backend.services.common.PackageCommon.PackageToSend
import scalaz._
import Scalaz._

trait PackageService extends PackageManagerService with PackageAckService with RpcService {
  self: ApiHandlerActor =>

  def handleMessage(p: Package, m: MessageBox): Unit = {
    acknowledgeReceivedPackage(p, m)
    m.body match { // TODO: move into pluggable traits
      case Ping(randomId) =>
        val reply = p.replyWith(m.messageId, Pong(randomId)).right
        handleActor ! PackageToSend(reply)
      case MessageAck(mids) =>
        ackTracker ! RegisterMessageAcks(mids.toList)
      case RpcRequestBox(body) =>
        handleRpc(p, m.messageId)(body)
      case _ =>
    }
  }

  def handlePackage(p: Package, pMsg: Option[TransportMessage]): Unit = {
    pMsg match {
      case Some(m) =>
        log.info(s"m: $m")
        //        val messageId = p.messageBox.messageId * System.currentTimeMillis() // TODO
        val messageId = p.messageBox.messageId
        handleActor ! PackageToSend(p.replyWith(messageId, m).right)
      case None =>
    }

    if (p.authId == 0L && p.sessionId == 0L) {
      val reply = p.replyWith(p.messageBox.messageId, ResponseAuthId(getAuthId)).right
      handleActor ! PackageToSend(reply)
    } else {
      p.messageBox.body match {
        case c@Container(_) => c.messages.foreach(handleMessage(p, _))
        case _ => handleMessage(p, p.messageBox)
      }
    }
  }

  def handleError(e: HandleError): Unit = e match {
    case ParseError(msg) =>
      val reply = Package(0L, 0L, MessageBox(0L, Drop(0L, msg))).left
      handleActor ! PackageToSend(reply)
    case _ => log.error("unknown handle error")
  }
}
