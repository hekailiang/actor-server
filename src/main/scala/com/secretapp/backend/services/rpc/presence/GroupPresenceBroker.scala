package com.secretapp.backend.services.rpc.presence

import akka.actor._
import akka.contrib.pattern.{ ClusterSharding, DistributedPubSubExtension, ShardRegion }
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import akka.persistence._
import com.secretapp.backend.data.message.{ update => updateProto }
import scala.collection.immutable
import scala.concurrent.duration._

object GroupPresenceProtocol {
  sealed trait PresenceMessage
  case class UserOnline(userId: Int, timeout: Long) extends PresenceMessage
  case class UserOffline(userId: Int) extends PresenceMessage
  case class TellPresences(target: ActorRef) extends PresenceMessage

  case class Envelope(chatId: Int, payload: PresenceMessage)
}

object GroupPresenceBroker {
  import GroupPresenceProtocol._

  def topicFor(chatId: Int): String = s"presences-g${chatId}"

  private val idExtractor: ShardRegion.IdExtractor = {
    case Envelope(chatId, msg) => (s"g${chatId}", msg)
  }

  private val shardCount = 2 // TODO: configurable

  private val shardResolver: ShardRegion.ShardResolver = {
    // TODO: better balancing
    case Envelope(id, msg) => (id % shardCount).toString
  }

  def startRegion()(implicit system: ActorSystem) =
    ClusterSharding(system).start(
      typeName = "GroupPresence",
      entryProps = Some(Props(classOf[GroupPresenceBroker])),
      idExtractor = idExtractor,
      shardResolver = shardResolver
    )
}

class GroupPresenceBroker extends Actor with ActorLogging {
  import GroupPresenceProtocol._
  import context.dispatcher
  import context.system

  val mediator = DistributedPubSubExtension(context.system).mediator

  var onlineUserIds = immutable.Set.empty[Int]
  var scheduledOffline = immutable.Map.empty[Int, Cancellable]

  val selfChatId = Integer.parseInt(self.path.name.drop(1))

  def receive = {
    case m @ UserOnline(userId, timeout) =>
      scheduledOffline.get(userId) match {
        case Some(scheduled) =>
          scheduled.cancel()
        case None =>
      }

      scheduledOffline += Tuple2(userId, system.scheduler.scheduleOnce(timeout.millis, self, UserOffline(userId)))

      if (!onlineUserIds.contains(userId)) {
        log.debug(s"Setting group online for chatId=$selfChatId userId=$userId")

        onlineUserIds += userId
        mediator ! Publish(GroupPresenceBroker.topicFor(selfChatId), updateProto.GroupOnline(selfChatId, onlineUserIds.size))
      }
    case UserOffline(userId) =>
      if (onlineUserIds.contains(userId)) {
        onlineUserIds -= userId
        mediator ! Publish(GroupPresenceBroker.topicFor(selfChatId), updateProto.GroupOnline(selfChatId, onlineUserIds.size))
      }
    case TellPresences(target) =>
      log.debug(s"Telling presences $onlineUserIds")
      mediator ! Publish(GroupPresenceBroker.topicFor(selfChatId), updateProto.GroupOnline(selfChatId, onlineUserIds.size))
  }
}
