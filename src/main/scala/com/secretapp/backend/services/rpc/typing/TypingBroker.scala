package com.secretapp.backend.services.rpc.typing

import akka.actor._
import akka.contrib.pattern.{ ClusterSharding, DistributedPubSubExtension, ShardRegion }
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import akka.persistence._
import com.secretapp.backend.data.message.{ update => updateProto }
import scala.collection.immutable
import scala.concurrent.duration._

object TypingType {
  val Text = 0
  val Photo = 1
  val Video = 2
  val Audio = 3
  val Document = 4
}

object TypingProtocol {
  sealed trait TypingMessage
  case class UserTyping(userId: Int, typingType: Int) extends TypingMessage
  case class UserNotTyping(userId: Int, typingType: Int) extends TypingMessage
  case class TellTypings(target: ActorRef) extends TypingMessage

  case class Envelope(userId: Int, payload: TypingMessage)
}

// TODO: rename to WeakUpdatesBroker
object TypingBroker {
  import TypingProtocol._
  import TypingType._

  def topicFor(userId: Int): String = s"typing-${userId}"
  def timeoutFor(typingType: Int) = typingType match {
    case Text     => 7.seconds
    case Photo    => 15.seconds
    case Video    => 15.seconds
    case Audio    => 15.seconds
    case Document => 15.seconds
  }

  private val idExtractor: ShardRegion.IdExtractor = {
    case Envelope(userId, msg) => (s"${userId}", msg)
  }

  private val shardCount = 2 // TODO: configurable

  private val shardResolver: ShardRegion.ShardResolver = {
    // TODO: better balancing
    case Envelope(userId, msg) => (userId % shardCount).toString
  }

  def startRegion()(implicit system: ActorSystem) =
    ClusterSharding(system).start(
      typeName = "Typing",
      entryProps = Some(Props(classOf[TypingBroker])),
      idExtractor = idExtractor,
      shardResolver = shardResolver
    )
}

class TypingBroker extends Actor with ActorLogging {
  import TypingProtocol._
  import context.dispatcher
  import context.system

  val mediator = DistributedPubSubExtension(context.system).mediator

  val selfUserId = Integer.parseInt(self.path.name)
  val topic = TypingBroker.topicFor(selfUserId)

  var typingUsers = immutable.Map.empty[(Int, Int), Cancellable]

  case object Stop

  context.setReceiveTimeout(15.minutes)

  def receive: Receive = {
    case m @ UserTyping(userId, typingType) =>
      typingUsers.get((userId, typingType)) match {
        case Some(scheduled) =>
          scheduled.cancel()
        case None =>
          log.debug(s"Publishing UserTyping ${userId}")
          mediator ! Publish(topic, updateProto.Typing(userId, typingType))
      }

      typingUsers += Tuple2((userId, typingType), system.scheduler.scheduleOnce(TypingBroker.timeoutFor(typingType), self, UserNotTyping(userId, typingType)))

    case m @ UserNotTyping(uid, typingType) =>
      typingUsers.get((uid, typingType)) map (_.cancel)
      typingUsers -= Tuple2(uid, typingType)

    case m @ TellTypings(target) =>
      typingUsers foreach {
        case ((userId, typingType), _) =>
          target ! updateProto.Typing(userId, typingType)
      }

    case ReceiveTimeout => context.parent ! ShardRegion.Passivate(stopMessage = Stop)
    case Stop           => context.stop(self)
  }
}
