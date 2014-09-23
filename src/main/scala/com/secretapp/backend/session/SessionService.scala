package com.secretapp.backend.session

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Subscribe
import com.secretapp.backend.api.ApiBrokerProtocol
import com.secretapp.backend.api.UpdatesBroker
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.data.transport.MTPackage
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

  var subscribedToUpdates = false
  var subscribingToUpdates = false

  protected def handleMessage(connector: ActorRef, mb: MessageBox): Unit = {
    acknowledgeReceivedPackage(connector, mb)

    mb.body match { // TODO: move into pluggable traits
      case Ping(randomId) =>
        val reply = mb.replyWith(authId, sessionId, Pong(randomId)).right
        connector.tell(reply, context.self)
      case MessageAck(mids) =>
        ackTracker.tell(RegisterMessageAcks(mids.toList), context.self)
      case RpcRequestBox(body) =>
        apiBroker.tell(ApiBrokerRequest(connector, mb.messageId, body), context.self)
      case _ =>
    }
  }

  protected def subscribeToUpdates() = {
    subscribingToUpdates = true
    log.info("Subscribing to updates authId={}", authId)
    mediator ! Subscribe(UpdatesBroker.topicFor(authId), updatesPusher)
  }

  protected def handleSubscribeAck(subscribe: Subscribe) = {
    log.info(s"Handling subscribe ack $subscribe")
    if (subscribe.topic == UpdatesBroker.topicFor(authId) && subscribe.ref == updatesPusher) {
      subscribingToUpdates = false
      subscribedToUpdates = true
    }
  }
}
