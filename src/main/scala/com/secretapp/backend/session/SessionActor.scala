package com.secretapp.backend.session

import akka.actor._
import akka.contrib.pattern.{ DistributedPubSubExtension, ClusterSharding, ShardRegion }
import akka.contrib.pattern.DistributedPubSubMediator.SubscribeAck
import akka.persistence._
import akka.util.{ ByteString, Timeout }
import com.secretapp.backend.data.message.{ Container, Drop, ResponseAuthId, RpcRequestBox, RpcResponseBox, UnsentResponse }
import com.secretapp.backend.data.models.User
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import com.secretapp.backend.services.SessionManager
import com.secretapp.backend.services.common.RandomService
import scala.concurrent.duration._
import scala.collection.immutable
import com.secretapp.backend.data.transport.MessageBox
import com.datastax.driver.core.{ Session => CSession }
import scodec.bits._
import com.secretapp.backend.data.transport.MTPackage
import com.secretapp.backend.protocol.transport._
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.api._
import com.secretapp.backend.api.rpc._
import com.secretapp.backend.data.message.{ Pong, Ping }
import com.secretapp.backend.data._
import PackageCommon._
import scalaz._
import Scalaz._
import com.datastax.driver.core.{ Session => CSession }

object SessionProtocol {
  // TODO: wrap all messages into Envelope
  sealed trait SessionMessage
  case class NewConnection(connector: ActorRef) extends SessionMessage
  case class HandleMessageBox(mb: MessageBox) extends SessionMessage
  case class AuthorizeUser(user: User) extends SessionMessage
  case class SendMessageBox(connector: ActorRef, mb: MessageBox) extends SessionMessage
  case object SubscribeToUpdates extends SessionMessage
  case class SubscribeToPresences(uids: immutable.Seq[Int]) extends SessionMessage
  case class UnsubscribeToPresences(uids: immutable.Seq[Int]) extends SessionMessage

  case class Envelope(authId: Long, sessionId: Long, payload: SessionMessage)
}

object SessionActor {
  import SessionProtocol._

  private val idExtractor: ShardRegion.IdExtractor = {
    case Envelope(authId, sessionId, msg) => (s"${authId}_${sessionId}", msg)
  }

  private val shardCount = 2 // TODO: configurable

  private val shardResolver: ShardRegion.ShardResolver = {
    // TODO: better balancing
    case Envelope(authId, sessionId, msg) => (authId * sessionId % shardCount).toString
  }

  def startRegion()(implicit system: ActorSystem, singletons: Singletons, clusterProxies: ClusterProxies, session: CSession) =
    ClusterSharding(system).start(
      typeName = "Session",
      entryProps = Some(Props(new SessionActor(singletons, clusterProxies, session))),
      idExtractor = idExtractor,
      shardResolver = shardResolver
    )

}

class SessionActor(val singletons: Singletons, val clusterProxies: ClusterProxies, session: CSession) extends SessionService with PersistentActor with PackageAckService with RandomService with ActorLogging {
  import ShardRegion.Passivate
  import SessionProtocol._
  import AckTrackerProtocol._
  import context.dispatcher

  case object Stop

  context.setReceiveTimeout(15.minutes)

  implicit val timeout = Timeout(5.seconds)
  val maxResponseLength = 1024 * 1024 // if more, register UnsentResponse for resend

  override def persistenceId: String = self.path.parent.name + "-" + self.path.name

  val splitName = self.path.name.split("_")
  override val authId = java.lang.Long.parseLong(splitName(0))
  override val sessionId = java.lang.Long.parseLong(splitName(1))

  val mediator = DistributedPubSubExtension(context.system).mediator
  val commonUpdatesPusher = context.actorOf(Props(new SeqPusherActor(context.self, authId)(session)))
  val weakUpdatesPusher = context.actorOf(Props(new WeakPusherActor(context.self, authId)))

  var connectors = immutable.Set.empty[ActorRef]
  var lastConnector: Option[ActorRef] = None

  // we need lazy here because subscribedToUpdates sets during receiveRecover
  lazy val apiBroker = context.actorOf(Props(new ApiBrokerActor(authId, sessionId, singletons, clusterProxies, subscribedToUpdates, session)), "api-broker")

  val receiveCommand: Receive = {
    case NewConnection(connector) =>
      log.debug(s"NewConnection ${connector}")
      connectors = connectors + connector
      lastConnector = Some(connector)
      context.watch(connector)

      getUnsentMessages() map { messages =>
        log.debug(s"unsent messages $messages")
        messages foreach { case Tuple2(mid, message) =>
          val pe = MTPackage(authId, sessionId, BitVector(message)).right
          connector ! pe
        }
      }
    case HandleMessageBox(mb) =>
      val connector = sender()
      log.debug(s"HandleMessageBox $authId $sessionId $mb $connector")
      lastConnector = Some(connector)

      mb.body match {
        case c@Container(_) => c.messages foreach (handleMessage(connector, _))
        case _ => handleMessage(connector, mb)
      }
    case SendMessageBox(connector, mb) =>
      log.debug(s"SendMessageBox $authId $sessionId $connector $mb")

      val origEncoded = MessageBoxCodec.encodeValid(mb)
      val origLength = origEncoded.length

      val encoded = mb.body match {
        case RpcResponseBox(messageId, _) if origLength > maxResponseLength =>
          val unsentResponse = UnsentResponse(mb.messageId, messageId, origLength.toInt)
          log.debug(s"Response is too large, generated $unsentResponse")
          MessageBoxCodec.encodeValid(
            MessageBox(
              mb.messageId,
              unsentResponse
            )
          )
        case _ => origEncoded
      }

      registerSentMessage(mb, ByteString(encoded.toByteArray))
      val pe = MTPackage(authId, sessionId, encoded).right

      connector ! pe
    case UpdateBoxToSend(ub) =>
      log.debug(s"UpdateBoxToSend $authId $sessionId $ub")
      // FIXME: real message id SA-32
      val mb = MessageBox(rand.nextLong, ub)
      val encoded = MessageBoxCodec.encodeValid(mb)
      val pe = MTPackage(authId, sessionId, encoded).right
      connectors foreach (_ ! pe)
      registerSentMessage(mb, ByteString(encoded.toByteArray))
    case msg @ AuthorizeUser(user) =>
      persist(msg) { _ =>
        apiBroker ! ApiBrokerProtocol.AuthorizeUser(user)
      }
    case msg @ SubscribeToUpdates =>
      if (!subscribedToUpdates && !subscribingToUpdates) {
        persist(msg) { _ =>
          subscribeToUpdates()
        }
      }
    case msg @ SubscribeToPresences(uids) =>
      persist(msg) { _ =>
        subscribeToPresences(uids)
      }
    case msg @ UnsubscribeToPresences(uids) =>
      persist(msg) { _ =>
        unsubscribeToPresences(uids)
      }
    case SubscribeAck(ack) =>
      handleSubscribeAck(ack)
    case Terminated(connector) =>
      log.debug(s"removing connector $connector")
      connectors = connectors - connector
    case ReceiveTimeout ⇒
      context.parent ! Passivate(stopMessage = Stop)
    case Stop =>
      context.stop(self)
    case x =>
      throw new Exception(s"Unhandled session message ${x} ${sender}")
  }

  val receiveRecover: Receive = {
    case RecoveryCompleted =>
      if (subscribingToUpdates) {
        subscribeToUpdates()
      }

      subscribeToPresences(subscribedToPresencesUids.toList)
      currentUser map { user =>
        apiBroker ! ApiBrokerProtocol.AuthorizeUser(user)
      }
    case AuthorizeUser(user) =>
      currentUser = Some(user)
    case SubscribeToUpdates =>
      subscribingToUpdates = true
    case SubscribeToPresences(uids) =>
      subscribedToPresencesUids = subscribedToPresencesUids ++ uids
    case UnsubscribeToPresences(uids) =>
      subscribedToPresencesUids = subscribedToPresencesUids -- uids
    case _ =>
  }
}
