package com.secretapp.backend.services.rpc.presence

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{ Subscribe, SubscribeAck, Unsubscribe }
import com.secretapp.backend.data.message.struct.UserId
import com.secretapp.backend.data.message.{ RpcResponseBox, UpdateBox }
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.rpc.{ Error, Ok, RpcResponse }
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.data.transport.MTPackage
import com.secretapp.backend.services.common.PackageCommon._
import scala.collection.immutable
import scala.concurrent.Future
import scalaz._
import Scalaz._

trait HandlerService {
  this: Handler =>

  import context.system
  import context.dispatcher
  import PresenceProtocol._

  val mediator = DistributedPubSubExtension(context.system).mediator
  val updatesPusher = context.actorOf(Props(new PusherActor(sessionActor, currentUser.authId)))
  var subscribedTo = immutable.Set.empty[Int]

  // TODO: check accessHash
  protected def handleSubscribeForOnline(users: immutable.Seq[UserId]) = {
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

  protected def handleUnsubscribeForOnline(users: immutable.Seq[UserId]) = {
    users foreach { userId =>
      subscribedTo = subscribedTo - userId.uid
      mediator ! Unsubscribe(PresenceBroker.topicFor(userId.uid), updatesPusher)
    }
  }

  protected def handleRequestSetOnline(isOnline: Boolean, timeout: Long): Future[RpcResponse] = {
    log.info(s"Handling RequestSetOnline ${isOnline} ${timeout}")
    val message = if (isOnline) {
      UserOnline(currentUser.uid, timeout)
    } else {
      UserOffline(currentUser.uid)
    }

    presenceBrokerProxy ! message

    Future.successful(Ok(ResponseOnline()))
  }
}

sealed class PusherActor(sessionActor: ActorRef, authId: Long) extends Actor with ActorLogging {
  import PresenceProtocol._

  def receive = {
    case u: UserOnlineUpdate =>
      log.info(s"Pushing presence to session authId=${authId} ${u}")
      val upd = WeakUpdate(System.currentTimeMillis / 1000, u)
      val ub = UpdateBox(upd)
      sessionActor ! UpdateBoxToSend(ub)
    case u: UserOfflineUpdate =>
      log.info(s"Pushing presence to session authId=${authId} ${u}")
      val upd = WeakUpdate(System.currentTimeMillis / 1000, u)
      val ub = UpdateBox(upd)
      sessionActor ! UpdateBoxToSend(ub)
    case u: UserLastSeenUpdate =>
      log.info(s"Pushing presence to session authId=${authId} ${u}")
      val upd = WeakUpdate(System.currentTimeMillis / 1000, u)
      val ub = UpdateBox(upd)
      sessionActor ! UpdateBoxToSend(ub)
  }
}
