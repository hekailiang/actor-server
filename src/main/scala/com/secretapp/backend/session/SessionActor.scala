package com.secretapp.backend.session

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.SubscribeAck
import akka.contrib.pattern.ClusterSharding
import akka.contrib.pattern.ShardRegion
import akka.persistence._
import com.secretapp.backend.api.frontend._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.models.User
import com.secretapp.backend.protocol.codecs.message.{JsonMessageBoxCodec, MessageBoxCodec}
import com.secretapp.backend.services.common.RandomService
import scala.concurrent.duration._
import akka.util.{ ByteString, Timeout }
import scala.collection.immutable
import com.secretapp.backend.data.transport.{TransportPackage, JsonPackage, MessageBox, MTPackage}
import com.datastax.driver.core.{ Session => CSession }
import scodec.bits._
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.api._
import PackageCommon._
import scalaz._
import Scalaz._
import com.datastax.driver.core.{ Session => CSession }

object SessionProtocol {
  // TODO: wrap all messages into Envelope
  sealed trait SessionMessage

  sealed trait HandleMessageBox {
    val mb: MessageBox
  }
  @SerialVersionUID(1L)
  case class HandleMTMessageBox(mb: MessageBox) extends HandleMessageBox with SessionMessage
  @SerialVersionUID(1L)
  case class HandleJsonMessageBox(mb: MessageBox) extends HandleMessageBox with SessionMessage

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

  def startRegion()(implicit system: ActorSystem, clusterProxies: ClusterProxies, session: CSession) = ClusterSharding(system).start(
    typeName = "Session",
    entryProps = Some(Props(new SessionActor(clusterProxies, session))),
    idExtractor = idExtractor,
    shardResolver = shardResolver
  )

}

class SessionActor(val clusterProxies: ClusterProxies, session: CSession) extends TransportSerializers with SessionService with PersistentActor with PackageAckService with RandomService with ActorLogging with MessageIdGenerator {
  import ShardRegion.Passivate
  import SessionProtocol._
  import AckTrackerProtocol._
  import context.dispatcher

  case object Stop

  context.setReceiveTimeout(15.minutes)

  implicit val timeout = Timeout(5.seconds)

  override def persistenceId: String = self.path.parent.name + "-" + self.path.name

  val splitName = self.path.name.split("_")
  override val authId = java.lang.Long.parseLong(splitName(0))
  override val sessionId = java.lang.Long.parseLong(splitName(1))

  val mediator = DistributedPubSubExtension(context.system).mediator
  val commonUpdatesPusher = context.actorOf(Props(new CommonPusherActor(context.self, authId)))
  val weakUpdatesPusher = context.actorOf(Props(new WeakPusherActor(context.self, authId)))

  var connectors = immutable.HashSet.empty[ActorRef]
  var lastConnector: Option[ActorRef] = None

  // we need lazy here because subscribedToUpdates sets during receiveRecover
  lazy val apiBroker = context.actorOf(Props(new ApiBrokerActor(authId, sessionId, clusterProxies, subscribedToUpdates, session)), "api-broker")

  override def receive = {
    case handleBox: HandleMessageBox =>
      transport = handleBox match {
        case _: HandleJsonMessageBox => JsonConnection.some
        case _: HandleMTMessageBox => MTConnection.some
      }
      context.become(receiveCommand)
      receiveCommand(handleBox)
    case p: AuthorizeUser =>
      receiveCommand(p)
    case m =>
      log.error(s"received unmatched message: $m")
  }

  def checkNewConnection(connector: ActorRef) = {
    if (!connectors.contains(connector)) {
      log.debug(s"NewConnection $connector")
      connectors = connectors + connector
      lastConnector = connector.some
      context.watch(connector)

      getUnsentMessages() map { messages =>
        messages foreach { case Tuple2(mid, message) =>
          // TODO
          val pe = MTPackage(authId, sessionId, BitVector(message)).right
          println(s"connector ! pe: $pe")
          connector ! pe
        }
      }
    }
  }

  val receiveCommand: Receive = {
    case handleBox: HandleMessageBox =>
      val connector = sender()
      checkNewConnection(connector)
      log.debug(s"HandleMessageBox ${handleBox.mb} $connector")
      lastConnector = connector.some

      handleBox.mb.body match {
        case c@Container(_) => c.messages foreach (handleMessage(connector, _))
        case _ => handleMessage(connector, handleBox.mb)
      }
    case SendMessageBox(connector, mb) =>
      log.debug(s"SendMessageBox $connector $mb")

      val blob = serializeMessageBox(mb)
      registerSentMessage(mb, ByteString(blob.toByteArray))

      val pe = serializePackage(blob)
      println(s"tell.! connector $connector ! pe: ${mb}")
      connector ! pe
    case UpdateBoxToSend(ub) =>
      log.debug(s"UpdateBoxToSend($ub)")
      // FIXME: real message id SA-32
      val mb = MessageBox(rand.nextLong, ub)
      println(s"tell.! connectors $connectors ! pe: $mb")
      val blob = serializeMessageBox(mb)
      registerSentMessage(mb, ByteString(blob.toByteArray))
      val pe = serializePackage(blob)
      connectors foreach (_ ! pe)
    case AuthorizeUser(user) =>
      apiBroker ! ApiBrokerProtocol.AuthorizeUser(user)
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
  }

  val receiveRecover: Receive = {
    case RecoveryCompleted =>
      if (subscribingToUpdates) {
        subscribeToUpdates()
      }

      subscribeToPresences(subscribedToPresencesUids.toList)
    case SubscribeToUpdates =>
      subscribingToUpdates = true
    case SubscribeToPresences(uids) =>
      subscribedToPresencesUids = subscribedToPresencesUids ++ uids
    case UnsubscribeToPresences(uids) =>
      subscribedToPresencesUids = subscribedToPresencesUids -- uids
    case _ =>
  }
}
