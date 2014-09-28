package com.secretapp.backend.services.rpc.presence

import akka.actor._
import akka.contrib.pattern.{ ClusterSingletonManager, ClusterSingletonProxy, DistributedPubSubExtension }
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import akka.persistence._
import com.secretapp.backend.data.message.update._
import scala.collection.immutable
import scala.concurrent.duration._

object PresenceProtocol {
  sealed trait PresenceMessage
  case class UserOnline(uid: Int, timeout: Long) extends PresenceMessage
  case class UserOffline(uid: Int) extends PresenceMessage
  case class TellPresences(uids: immutable.Seq[Int], target: ActorRef) extends PresenceMessage
}

// TODO: rename to WeakUpdatesBroker
object PresenceBroker {
  def topicFor(userId: Int): String = s"presences-${userId}"

  def start(implicit system: ActorSystem) = {
    val props = ClusterSingletonManager.props(
      singletonProps = Props(new PresenceBroker),
      singletonName = "presence-broker",
      terminationMessage = PoisonPill,
      role = None // TODO: specify roles the singleton should run on
      )
    system.actorOf(props, name = "presence-singleton")
  }

  def startProxy(implicit system: ActorSystem) = {
    system.actorOf(
      ClusterSingletonProxy.props(
        singletonPath = "/user/presence-singleton/presence-broker",
        role = None),
      name = "presence-broker-proxy")
  }
}

class PresenceBroker extends PersistentActor with ActorLogging {
  import PresenceProtocol._
  import context.dispatcher
  import context.system

  val mediator = DistributedPubSubExtension(context.system).mediator
  var onlineUids = immutable.Map.empty[Int, Option[Cancellable]]
  var lastSeens = immutable.Map.empty[Int, Long]

  override def persistenceId: String = "presence-broker"

  def receiveCommand: Receive = {
    case m @ UserOnline(uid, timeout) =>
      onlineUids.get(uid) match {
        case Some(optScheduled) =>
          optScheduled map (_.cancel())
        case None =>
          log.info(s"Publishing UserOnline ${uid}")
          mediator ! Publish(PresenceBroker.topicFor(uid), UserOnlineUpdate(uid))
      }

      onlineUids = onlineUids + Tuple2(uid, Some(system.scheduler.scheduleOnce(timeout.millis, self, UserOffline(uid))))
    case m @ UserOffline(uid) =>
      val currentTime = System.currentTimeMillis / 1000

      persist(Tuple2(m, currentTime)) { _ =>
        onlineUids.get(uid).flatten map (_.cancel())
        onlineUids = onlineUids - uid
        lastSeens = lastSeens + Tuple2(uid, currentTime)
        mediator ! Publish(PresenceBroker.topicFor(uid), UserOfflineUpdate(uid))
      }
    case TellPresences(uids, target) =>
      log.info(s"TellPresences ${uids} ${target}")
      uids foreach { uid =>
        if (onlineUids.contains(uid)) {
          target ! UserOnlineUpdate(uid)
        } else {
          lastSeens.get(uid) match {
            case Some(time) =>
              target ! UserLastSeenUpdate(uid, time)
            case None =>
          }
        }
      }
  }

  def receiveRecover: Receive = {
    case (UserOffline(uid), time: Long) =>
      lastSeens = lastSeens + Tuple2(uid, time)
  }
}
