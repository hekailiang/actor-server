package com.secretapp.backend.session

import akka.actor.ActorRef
import com.secretapp.backend.api.ApiBrokerProtocol
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist.AuthIdRecord
import com.secretapp.backend.protocol.transport._
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.data.message._
import scalaz._
import Scalaz._

trait SessionService {
  self: SessionActor =>
  import AckTrackerProtocol._
  import ApiBrokerProtocol._

  def handleMessage(connector: ActorRef, p: Package, m: MessageBox): Unit = {
    acknowledgeReceivedPackage(connector, p, m)

    m.body match { // TODO: move into pluggable traits
      case Ping(randomId) =>
        val reply = p.replyWith(m.messageId, Pong(randomId)).right
        connector.tell(reply, context.self)
      case MessageAck(mids) =>
        ackTracker.tell(RegisterMessageAcks(mids.toList), context.self)
      case RpcRequestBox(body) =>
        apiBroker.tell(ApiBrokerRequest(connector, p.messageBox.messageId, body), context.self)
      case _ =>
    }
  }
}
