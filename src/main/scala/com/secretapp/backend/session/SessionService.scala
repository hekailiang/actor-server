package com.secretapp.backend.session

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{ Subscribe, Unsubscribe }
import com.secretapp.backend.api.ApiBrokerProtocol
import com.secretapp.backend.api.UpdatesBroker
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.data.transport.MTPackage
import com.secretapp.backend.persist.AuthIdRecord
import com.secretapp.backend.protocol.transport._
import com.secretapp.backend.services.UserManagerService
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.data.message._
import com.secretapp.backend.services.rpc.presence.{ PresenceBroker, PresenceProtocol }
import com.secretapp.backend.services.rpc.typing.{ TypingBroker, TypingProtocol }
import scala.collection.immutable
import scala.concurrent.duration._
import scalaz._
import Scalaz._

trait SessionService extends UserManagerService {
  self: SessionActor =>
  import AckTrackerProtocol._
  import ApiBrokerProtocol._

  import context.dispatcher

  var subscribedToUpdates = false
  var subscribingToUpdates = false

  var subscribedToPresencesUids = immutable.Set.empty[Int]

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
      case x =>
        log.error("unhandled session message $x")
    }
  }

  protected def subscribeToPresences(uids: immutable.Seq[Int]) = {
    uids foreach { uid =>
      if (!subscribedToPresencesUids.contains(uid)) {
        log.info(s"Subscribing $uid")
        subscribedToPresencesUids = subscribedToPresencesUids + uid
        mediator ! Subscribe(PresenceBroker.topicFor(uid), weakUpdatesPusher)
      } else {
        log.error(s"Already subscribed to $uid")
      }
    }

    clusterProxies.presenceBroker ! PresenceProtocol.TellPresences(uids, weakUpdatesPusher)
  }

  protected def unsubscribeToPresences(uids: immutable.Seq[Int]) = {
    uids foreach { uid =>
      subscribedToPresencesUids = subscribedToPresencesUids - uid
      mediator ! Unsubscribe(PresenceBroker.topicFor(uid), weakUpdatesPusher)
    }
  }

  protected def subscribeToUpdates() = {
    subscribingToUpdates = true
    log.info("Subscribing to updates authId={}", authId)
    mediator ! Subscribe(UpdatesBroker.topicFor(authId), commonUpdatesPusher)

    subscribeToTypings()
  }

  protected def subscribeToTypings(): Unit = {
    if (currentUser.isDefined) {
      mediator ! Subscribe(TypingBroker.topicFor(currentUser.get.uid), weakUpdatesPusher)
      singletons.typingBrokerRegion ! TypingProtocol.Envelope(
        currentUser.get.uid,
        TypingProtocol.TellTypings(weakUpdatesPusher)
      )
    } else { // wait for AuthorizeUser message
      log.debug("Waiting for AuthorizeUser and try to subscribe to typings again")
      context.system.scheduler.scheduleOnce(500.milliseconds) {
        subscribeToTypings()
      }
    }
  }

  protected def handleSubscribeAck(subscribe: Subscribe) = {
    log.info(s"Handling subscribe ack $subscribe")
    if (subscribe.topic == UpdatesBroker.topicFor(authId) && subscribe.ref == commonUpdatesPusher) {
      subscribingToUpdates = false
      subscribedToUpdates = true
    } else if (subscribe.topic.startsWith("presences-") && subscribe.ref == commonUpdatesPusher) {
      // FIXME: don't use startsWith here

      // TODO: implement ack handling
    }
  }
}
