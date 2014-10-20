package com.secretapp.backend.services.rpc.presence

import akka.actor._
import akka.contrib.pattern.{ ClusterSharding, DistributedPubSubExtension, ShardRegion }
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import akka.persistence._
import com.secretapp.backend.data.message.{ update => updateProto }
import scala.collection.immutable
import scala.concurrent.duration._

object PresenceType {
  val Offline = 0
  val Online = 1
}

object PresenceProtocol {
  sealed trait PresenceMessage
  case class UserOnline(timeout: Long) extends PresenceMessage
  case object UserOffline extends PresenceMessage
  case class TellPresence(target: ActorRef) extends PresenceMessage

  case class Envelope(userId: Int, payload: PresenceMessage)
}

object PresenceBroker {
  import PresenceProtocol._

  def topicFor(userId: Int): String = s"presences-u${userId}"

  private val idExtractor: ShardRegion.IdExtractor = {
    case Envelope(userId, msg) => (s"u${userId}", msg)
  }

  private val shardCount = 2 // TODO: configurable

  private val shardResolver: ShardRegion.ShardResolver = {
    // TODO: better balancing
    case Envelope(id, msg) => (id % shardCount).toString
  }

  def startRegion()(implicit system: ActorSystem) =
    ClusterSharding(system).start(
      typeName = "Presence",
      entryProps = Some(Props(classOf[PresenceBroker])),
      idExtractor = idExtractor,
      shardResolver = shardResolver
    )
}

class PresenceBroker extends PersistentActor with ActorLogging {
  import PresenceProtocol._
  import context.dispatcher
  import context.system

  val mediator = DistributedPubSubExtension(context.system).mediator

  var lastSeen: Option[Long] = None
  var presence = PresenceType.Offline
  var scheduledOffline: Option[Cancellable] = None

  val selfUserId = Integer.parseInt(self.path.name.drop(1))
  override def persistenceId = s"presence-broker-u$selfUserId"

  type State = Option[Long]

  def receiveCommand: Receive = {
    case m @ UserOnline(timeout) =>
      val time = System.currentTimeMillis / 1000

      saveSnapshot(Some(time))

      setState(Some(time))

      scheduledOffline map (_.cancel())
      scheduledOffline = Some(system.scheduler.scheduleOnce(timeout.millis, self, UserOffline))

      if (presence != PresenceType.Online) {
        publish(PresenceBroker.topicFor(selfUserId), updateProto.UserOnline(selfUserId))
      }

      presence = PresenceType.Online
    case UserOffline =>
      presence = PresenceType.Offline
      val update = lastSeen match {
        case Some(time) =>
          updateProto.UserLastSeen(selfUserId, time)
        case None => // should never happen but shit happens
          updateProto.UserOffline(selfUserId)
      }
      publish(PresenceBroker.topicFor(selfUserId), update)
    case TellPresence(target) =>
      val update = presence match {
        case PresenceType.Online =>
          updateProto.UserOnline(selfUserId)
        case PresenceType.Offline =>
          lastSeen match {
            case Some(time) =>
              updateProto.UserLastSeen(selfUserId, time)
            case None => updateProto.UserOffline(selfUserId)
          }
      }
      target ! update
  }

  def receiveRecover: Receive = {
    case SnapshotOffer(_, state: State) =>
      setState(state)
  }

  def setState(state: State) = {
    lastSeen = state
  }

  def publish(topic: String, update: updateProto.WeakUpdateMessage) = {
    log.debug(s"Publishing to $topic $update")
    mediator ! Publish(topic, update)
  }
}
