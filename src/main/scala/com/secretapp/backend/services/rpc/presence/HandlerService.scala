package com.secretapp.backend.services.rpc.presence

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{ Subscribe, SubscribeAck, Unsubscribe }
import com.secretapp.backend.data.message.struct.UserId
import com.secretapp.backend.data.message.{ RpcResponseBox, UpdateBox }
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.rpc.{ Error, Ok }
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.services.common.PackageCommon._
import scala.collection.immutable
import scalaz._
import Scalaz._

trait HandlerService {
  this: Handler =>

  import context.system
  import context.dispatcher
  import PresenceProtocol._

  val mediator = DistributedPubSubExtension(context.system).mediator
  val updatesPusher = context.actorOf(Props(new PusherActor(handleActor, currentUser.authId)))
  var subscribedTo = immutable.Set.empty[Int]

  // TODO: check accessHash
  protected def handleSubscribeForOnline(p: Package, messageId: Long)(users: immutable.Seq[UserId]) = {
    log.info(s"handling SubscribeForOnline ${users}")
    users foreach { userId =>
      if (!subscribedTo.contains(userId.uid)) {
        log.info(s"Subscribing ${userId.uid}")
        subscribedTo = subscribedTo + userId.uid
        mediator ! Subscribe(PresenceBroker.topicFor(userId.uid), updatesPusher)
      }
    }

    presenceBrokerProxy ! TellPresences(users map (_.uid), updatesPusher)
  }

  protected def handleUnsubscribeForOnline(p: Package, messageId: Long)(users: immutable.Seq[UserId]) = {
    users foreach { userId =>
      subscribedTo = subscribedTo - userId.uid
      mediator ! Unsubscribe(PresenceBroker.topicFor(userId.uid), updatesPusher)
    }
  }

  protected def handleRequestSetOnline(p: Package, messageId: Long)(isOnline: Boolean, timeout: Long) = {
    log.info(s"Handling RequestSetOnline ${isOnline} ${timeout}")
    val message = if (isOnline) {
      UserOnline(currentUser.uid, timeout)
    } else {
      UserOffline(currentUser.uid)
    }

    presenceBrokerProxy ! message

    handleActor ! PackageToSend(p.replyWith(messageId, RpcResponseBox(messageId, Ok(ResponseOnline()))).right)
  }
}

sealed class PusherActor(handleActor: ActorRef, authId: Long) extends Actor with ActorLogging {
  import PresenceProtocol._

  def receive = {
    case u: UserOnlineUpdate =>
      log.info(s"Pushing presence to session authId=${authId} ${u}")
      val upd = WeakUpdate(System.currentTimeMillis / 1000, u)
      val ub = UpdateBox(upd)
      handleActor ! UpdateBoxToSend(ub)
    case u: UserOfflineUpdate =>
      log.info(s"Pushing presence to session authId=${authId} ${u}")
      val upd = WeakUpdate(System.currentTimeMillis / 1000, u)
      val ub = UpdateBox(upd)
      handleActor ! UpdateBoxToSend(ub)
    case u: UserLastSeenUpdate =>
      log.info(s"Pushing presence to session authId=${authId} ${u}")
      val upd = WeakUpdate(System.currentTimeMillis / 1000, u)
      val ub = UpdateBox(upd)
      handleActor ! UpdateBoxToSend(ub)
  }
}
