package com.secretapp.backend.services.rpc.typing

import akka.actor._
import akka.contrib.pattern.{ ClusterSharding, DistributedPubSubExtension, ShardRegion }
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import akka.persistence._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.persist.GroupChatUserRecord
import com.secretapp.backend.helpers.UserHelpers
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
  case class GroupEnvelope(userId: Int, payload: TypingMessage)

  case object BrokerType extends Enumeration {
    val User = Value(0)
    val Group = Value(1)
  }
}

// TODO: rename to WeakUpdatesBroker
object TypingBroker {
  import TypingProtocol._
  import TypingType._

  def topicFor(userId: Int): String = s"typing-u${userId}"
  def topicFor(userId: Int, authId: Long): String = s"typing-u${userId}-a${authId}"

  def topicFor(typ: BrokerType.Value, userId: Int): String = typ match {
    case BrokerType.User  => s"typing-u${userId}"
    case BrokerType.Group => s"typing-g${userId}"
  }

  def timeoutFor(typingType: Int) = typingType match {
    case Text     => 7.seconds
    case Photo    => 15.seconds
    case Video    => 15.seconds
    case Audio    => 15.seconds
    case Document => 15.seconds
  }

  private val idExtractor: ShardRegion.IdExtractor = {
    case Envelope(userId, msg) => (s"u${userId}", msg)
    case GroupEnvelope(chatId, msg) => (s"g${chatId}", msg)
  }

  private val shardCount = 2 // TODO: configurable

  private val shardResolver: ShardRegion.ShardResolver = {
    // TODO: better balancing
    case Envelope(userId, msg) => (userId % shardCount).toString
    case GroupEnvelope(chatId, msg) => (chatId % shardCount).toString
  }

  def startRegion()(implicit system: ActorSystem, session: CSession) =
    ClusterSharding(system).start(
      typeName = "Typing",
      entryProps = Some(Props(classOf[TypingBroker], session)),
      idExtractor = idExtractor,
      shardResolver = shardResolver
    )
}

class TypingBroker(implicit val session: CSession) extends Actor with ActorLogging with UserHelpers {
  import TypingProtocol._
  import context.dispatcher
  import context.system

  val mediator = DistributedPubSubExtension(context.system).mediator

  val selfType = if (self.path.name.startsWith("u")) {
    BrokerType.User
  } else if (self.path.name.startsWith("g")) {
    BrokerType.Group
  } else {
    throw new Exception(s"Unknown typing broker type ${self.path.name}")
  }

  val selfId = Integer.parseInt(self.path.name.drop(1))

  var typingUsers = immutable.Map.empty[(Int, Int), Cancellable]

  case object Stop

  context.setReceiveTimeout(15.minutes)

  def receive: Receive = {
    case m @ UserTyping(userId, typingType) =>
      typingUsers.get((userId, typingType)) match {
        case Some(scheduled) =>
          scheduled.cancel()
        case None =>

      }

      selfType match {
        case BrokerType.User =>
          log.debug(s"Publishing UserTyping ${userId}")
          mediator ! Publish(TypingBroker.topicFor(selfId), updateProto.Typing(userId, typingType))
        case BrokerType.Group =>
          log.debug(s"Publishing UserTypingGroup ${userId}")
          GroupChatUserRecord.getUsersWithKeyHashes(selfId) map { xs =>
            xs foreach {
              case (userId, keyHashes) =>
                keyHashes foreach { keyHash =>
                  for {
                    optAuthId <- authIdFor(userId, keyHash)

                  } yield {
                    optAuthId map { authId =>
                      mediator ! Publish(TypingBroker.topicFor(userId, authId), updateProto.TypingGroup(selfId, userId, typingType))
                    }
                  }
                }
            }
          }
      }

      typingUsers += Tuple2((userId, typingType), system.scheduler.scheduleOnce(TypingBroker.timeoutFor(typingType), self, UserNotTyping(userId, typingType)))

    case m @ UserNotTyping(uid, typingType) =>
      typingUsers.get((uid, typingType)) map (_.cancel)
      typingUsers -= Tuple2(uid, typingType)

    case m @ TellTypings(target) =>
      typingUsers foreach {
        case ((userId, typingType), _) =>
          selfType match {
            case BrokerType.User =>
              target ! updateProto.Typing(userId, typingType)
            case BrokerType.Group =>
              target ! updateProto.TypingGroup(selfId, userId, typingType)
          }
      }

    case ReceiveTimeout => context.parent ! ShardRegion.Passivate(stopMessage = Stop)
    case Stop           => context.stop(self)
  }
}
