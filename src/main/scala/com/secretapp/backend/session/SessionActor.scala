package com.secretapp.backend.session

import akka.actor._
import akka.persistence._
import akka.contrib.pattern.{ DistributedPubSubExtension, ClusterSharding, ShardRegion }
import akka.contrib.pattern.DistributedPubSubMediator.SubscribeAck
import akka.util.Timeout
import com.secretapp.backend.api.frontend._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.models.User
import com.secretapp.backend.services.common.RandomService
import scala.concurrent.duration._
import scala.collection.immutable
import com.secretapp.backend.data.transport.MessageBox
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
  case class SendRpcResponseBox(connector: ActorRef, rpcBox: RpcResponseBox) extends SessionMessage
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

class SessionActor(val singletons: Singletons, val clusterProxies: ClusterProxies, session: CSession) extends PersistentActor with TransportSerializers with SessionService with PackageAckService with RandomService with MessageIdGenerator with ActorLogging {
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
  val authId = java.lang.Long.parseLong(splitName(0))
  val sessionId = java.lang.Long.parseLong(splitName(1))

  val mediator = DistributedPubSubExtension(context.system).mediator
  val commonUpdatesPusher = context.actorOf(Props(new SeqPusherActor(context.self, authId)(session)))
  val weakUpdatesPusher = context.actorOf(Props(new WeakPusherActor(context.self, authId)))

  var connectors = immutable.HashSet.empty[ActorRef]
  var lastConnector: Option[ActorRef] = None

  // we need lazy here because subscribedToUpdates sets during receiveRecover
  lazy val apiBroker = context.actorOf(Props(new ApiBrokerActor(authId, sessionId, singletons, clusterProxies, subscribedToUpdates, session)), "api-broker")

  def queueNewSession(messageId: Long): Unit = {
    log.info(s"queueNewSession $messageId, $sessionId")
    val mb = MessageBox(getMessageId(TransportMsgId), NewSession(messageId, sessionId))
    registerSentMessage(mb, serializeMessageBox(mb))
  }

  def checkNewConnection(connector: ActorRef) = {
    if (!connectors.contains(connector)) {
      log.debug(s"NewConnection $connector")
      connectors = connectors + connector
      lastConnector = connector.some
      context.watch(connector)

      getUnsentMessages() map { messages =>
        messages foreach { case Tuple2(mid, message) =>
          val pe = serializePackage(message)
          println(s"connector ! pe: $pe")
          connector ! pe
        }
      }
    }
  }

  val receiveCommand: Receive = {
    case handleBox: HandleMessageBox =>
      transport = handleBox match {
        case _: HandleJsonMessageBox => JsonConnection.some
        case _: HandleMTMessageBox => MTConnection.some
      }
      queueNewSession(handleBox.mb.messageId)
      context.become(receiveBusinessLogic)
      receiveBusinessLogic(handleBox)
    case p: AuthorizeUser =>
      receiveBusinessLogic(p)
    case m =>
      log.error(s"received unmatched message: $m")
  }

  val receiveBusinessLogic: Receive = {
    case handleBox: HandleMessageBox =>
      val connector = sender()
      checkNewConnection(connector)
      log.debug(s"HandleMessageBox ${handleBox.mb} $connector")
      lastConnector = connector.some

      handleBox.mb.body match {
        case c@Container(_) => c.messages foreach (handleMessage(connector, _))
        case _ => handleMessage(connector, handleBox.mb)
      }
    case SendRpcResponseBox(connector, rpcBox) =>
      val mb = MessageBox(getMessageId(ResponseMsgId), rpcBox)
      log.debug(s"SendMessageBox $authId $sessionId $connector $mb")

      val origEncoded = serializeMessageBox(mb)
      val origLength = origEncoded.length
      val blob = mb.body match {
        case RpcResponseBox(messageId, _) if origLength > maxResponseLength =>
          val unsentResponse = UnsentResponse(mb.messageId, messageId, origLength.toInt)
          log.debug(s"Response is too large, generated $unsentResponse")
          serializeMessageBox(MessageBox(mb.messageId, unsentResponse))
        case _ => origEncoded
      }
      registerSentMessage(mb, blob)

      val pe = serializePackage(blob)
      println(s"tell.! connector $connector ! pe: $mb")
      connector ! pe
    case UpdateBoxToSend(ub) =>
      log.debug(s"UpdateBoxToSend $authId $sessionId $ub")
      val mb = MessageBox(getMessageId(UpdateMsgId), ub)
      println(s"tell.! connectors $connectors ! pe: $mb")
      val blob = serializeMessageBox(mb)
      registerSentMessage(mb, blob)
      val pe = serializePackage(blob)
      connectors foreach (_ ! pe)
    case msg @ AuthorizeUser(user) =>
      log.debug(s"$msg")
      persist(msg) { _ =>
        currentUser = Some(user)
        apiBroker ! ApiBrokerProtocol.AuthorizeUser(user)
      }
    case msg @ SubscribeToUpdates =>
      persist(msg) { _ =>
        if (!subscribedToUpdates && !subscribingToUpdates) {
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

      recoverSubscribeToPresences(subscribedToPresencesUids.toList)

      currentUser map { user =>
        apiBroker ! ApiBrokerProtocol.AuthorizeUser(user)
      }
    case AuthorizeUser(user) =>
      currentUser = Some(user)
      apiBroker ! ApiBrokerProtocol.AuthorizeUser(user)
    case SubscribeToUpdates =>
      subscribingToUpdates = true
    case SubscribeToPresences(uids) =>
      subscribedToPresencesUids = subscribedToPresencesUids ++ uids
    case UnsubscribeToPresences(uids) =>
      subscribedToPresencesUids = subscribedToPresencesUids -- uids
    case _ =>
  }
}
