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
import com.secretapp.backend.services.rpc.presence.{ GroupPresenceBroker, GroupPresenceProtocol, PresenceBroker, PresenceProtocol }
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
  var subscribedToPresencesChatIds = immutable.Set.empty[Int]

  protected def handleMessage(connector: ActorRef, mb: MessageBox): Unit = {
    acknowledgeReceivedPackage(connector, mb)

    mb.body match { // TODO: move into pluggable traits
      case Ping(randomId) =>
        val reply = serializePackage(MessageBox(getMessageId(TransportMsgId), Pong(randomId)))
        connector.tell(reply, context.self)
      case MessageAck(mids) =>
        ackTracker.tell(RegisterMessageAcks(mids.toList), context.self)
      case RpcRequestBox(body) =>
        apiBroker.tell(ApiBrokerRequest(connector, mb.messageId, body), context.self)
      case x =>
        log.error(s"unhandled session message $x")
    }
  }

  protected def subscribeToPresences(userIds: immutable.Seq[Int]) = {
    userIds foreach { userId =>
      if (!subscribedToPresencesUids.contains(userId)) {
        val topic = PresenceBroker.topicFor(userId)
        log.debug(s"Subscribing $userId $topic")
        subscribedToPresencesUids = subscribedToPresencesUids + userId
        mediator ! Subscribe(
          topic,
          weakUpdatesPusher
        )
      } else {
        log.warning(s"Already subscribed to $userId")
      }

      singletons.presenceBrokerRegion ! PresenceProtocol.Envelope(
        userId,
        PresenceProtocol.TellPresence(weakUpdatesPusher)
      )
    }
  }

  protected def recoverSubscribeToPresences(userIds: immutable.Seq[Int]) = {
    userIds foreach { userId =>
      log.info(s"Subscribing $userId")
      subscribedToPresencesUids = subscribedToPresencesUids + userId
      mediator ! Subscribe(
        PresenceBroker.topicFor(userId),
        weakUpdatesPusher
      )

      singletons.presenceBrokerRegion ! PresenceProtocol.Envelope(
        userId,
        PresenceProtocol.TellPresence(weakUpdatesPusher)
      )
    }
  }

  protected def unsubscribeToPresences(uids: immutable.Seq[Int]) = {
    uids foreach { userId =>
      subscribedToPresencesUids = subscribedToPresencesUids - userId
      mediator ! Unsubscribe(
        PresenceBroker.topicFor(userId),
        weakUpdatesPusher
      )
    }
  }

  protected def subscribeToGroupPresences(chatIds: immutable.Seq[Int]) = {
    chatIds foreach { chatId =>
      if (!subscribedToPresencesChatIds.contains(chatId)) {
        log.info(s"Subscribing to chat presences chatId=$chatId")
        subscribedToPresencesChatIds = subscribedToPresencesChatIds + chatId
        mediator ! Subscribe(
          GroupPresenceBroker.topicFor(chatId),
          weakUpdatesPusher
        )
      } else {
        log.error(s"Already subscribed to $chatId")
      }

      singletons.groupPresenceBrokerRegion ! GroupPresenceProtocol.Envelope(
        chatId,
        GroupPresenceProtocol.TellPresences(weakUpdatesPusher)
      )
    }
  }

  protected def recoverSubscribeToGroupPresences(chatIds: immutable.Seq[Int]) = {
    chatIds foreach { chatId =>
      log.info(s"Subscribing to chat presences chatId=$chatId")
      subscribedToPresencesChatIds = subscribedToPresencesChatIds + chatId
      mediator ! Subscribe(
        GroupPresenceBroker.topicFor(chatId),
        weakUpdatesPusher
      )

      singletons.groupPresenceBrokerRegion ! GroupPresenceProtocol.Envelope(
        chatId,
        GroupPresenceProtocol.TellPresences(weakUpdatesPusher)
      )
    }
  }

  protected def unsubscribeFromGroupPresences(chatIds: immutable.Seq[Int]) = {
    chatIds foreach { chatId =>
      subscribedToPresencesChatIds = subscribedToPresencesChatIds - chatId
      mediator ! Unsubscribe(
        GroupPresenceBroker.topicFor(chatId),
        weakUpdatesPusher
      )
    }
  }

  protected def subscribeToUpdates() = {
    subscribingToUpdates = true
    log.info(s"Subscribing to updates authId=$authId")
    mediator ! Subscribe(UpdatesBroker.topicFor(authId), commonUpdatesPusher)

    subscribeToTypings()
  }

  protected def subscribeToTypings(): Unit = {
    if (currentUser.isDefined) {
      mediator ! Subscribe(TypingBroker.topicFor(currentUser.get.uid), weakUpdatesPusher)
      mediator ! Subscribe(TypingBroker.topicFor(currentUser.get.uid, currentUser.get.authId), weakUpdatesPusher)

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
