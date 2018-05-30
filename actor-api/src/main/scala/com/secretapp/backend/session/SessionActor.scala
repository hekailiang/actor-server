package com.secretapp.backend.session

import akka.actor._
import akka.contrib.pattern.DistributedPubSubMediator.SubscribeAck
import akka.contrib.pattern.{ DistributedPubSubExtension, ClusterSharding, ShardRegion }
import akka.persistence._
import akka.util.Timeout
import com.secretapp.backend.api._
import com.secretapp.backend.api.frontend._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.update.WeakUpdate
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.models
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.services.common.RandomService
import im.actor.server.persist.file.adapter.FileAdapter
import im.actor.util.logging.MDCActorLogging
import scala.collection.immutable
import scala.concurrent.duration._
import scalaz.Scalaz._
import scodec.bits._

object SessionActor {
  import SessionProtocol._

  private val idExtractor: ShardRegion.IdExtractor = {
    case Envelope(authId, sessionId, msg) => (s"${authId}_${sessionId}", msg)
  }

  private val shardCount = 2 // TODO: configurable

  private val shardResolver: ShardRegion.ShardResolver = {
    // TODO: better balancing
    case Envelope(authId, sessionId, _) => (authId * sessionId % shardCount).abs.toString
  }

  def props(singletons: Singletons, updatesBrokerRegion: ActorRef, socialBrokerRegion: ActorRef, fileAdapter: FileAdapter, receiveTimeout: FiniteDuration) = {
    Props(new SessionActor(singletons, updatesBrokerRegion, socialBrokerRegion, fileAdapter, receiveTimeout))
  }

  def startRegion(
    singletons: Singletons,
    updatesBrokerRegion: ActorRef,
    socialBrokerRegion: ActorRef,
    fileAdapter: FileAdapter,
    receiveTimeout: FiniteDuration
  )(implicit system: ActorSystem) =
    ClusterSharding(system).start(
      typeName = "Session",
      entryProps = Some(props(singletons, updatesBrokerRegion, socialBrokerRegion, fileAdapter, receiveTimeout)),
      idExtractor = idExtractor,
      shardResolver = shardResolver
    )
}

class SessionActor(
  val singletons: Singletons,
  val updatesBrokerRegion: ActorRef,
  val socialBrokerRegion: ActorRef,
  val fileAdapter: FileAdapter,
  receiveTimeout: FiniteDuration
) extends PersistentActor
    with TransportSerializers
    with SessionService
    with PackageAckService
    with RandomService
    with MessageIdGenerator
    with MDCActorLogging {
  import ShardRegion.Passivate
  import SessionProtocol._
  import AckTrackerProtocol._
  import context.dispatcher

  case object Stop

  context.setReceiveTimeout(receiveTimeout)

  implicit val timeout = Timeout(5.seconds)
  val maxResponseLength = 1024 // if more, register UnsentResponse for resend

  override def persistenceId: String = self.path.parent.name + "-" + self.path.name

  val splitName = self.path.name.split("_")
  val authId = java.lang.Long.parseLong(splitName(0))
  val sessionId = java.lang.Long.parseLong(splitName(1))

  val mediator = DistributedPubSubExtension(context.system).mediator
  val seqUpdatesPusher = context.actorOf(Props(new SeqPusherActor(context.self, authId)))
  val weakUpdatesPusher = context.actorOf(Props(new WeakPusherActor(context.self, authId)))

  var connectors = immutable.HashSet.empty[ActorRef]
  var lastConnector: Option[ActorRef] = None

  // we need lazy here because subscribedToUpdates sets during receiveRecover
  lazy val apiBroker = context.actorOf(
    Props(
      classOf[ApiBrokerActor],
      authId, sessionId, singletons, fileAdapter, updatesBrokerRegion, socialBrokerRegion, subscribedToUpdates
    ), "api-broker"
  )

  override protected def mdc = Map(
    "unit" -> "session",
    "sessionId" -> sessionId,
    "authId" -> authId
  )

  def queueNewSession(messageId: Long): Unit = {
    withMDC(log.debug(s"$authId, $sessionId#queueNewSession $messageId, $sessionId"))
    val mb = MessageBox(getMessageId(TransportMsgId), NewSession(messageId, sessionId))
    registerSentMessage(mb, serializeMessageBox(mb))
  }

  override def preStart(): Unit = {
    withMDC(log.debug(s"$authId, $sessionId#preStart"))
    super.preStart()
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    withMDC(log.error(reason, s"Session is restarting due to error on handlong $message"))
    super.preRestart(reason, message)
  }

  override def postStop(): Unit = {
    withMDC(log.debug(s"postStop"))
    connectors foreach (_ ! SilentClose)
    super.postStop()
  }

  def checkNewConnection(connector: ActorRef) = {
    if (!connectors.contains(connector)) {
      withMDC(log.debug(s"NewConnection $connector"))
      connectors = connectors + connector
      lastConnector = connector.some
      context.watch(connector)

      getUnsentMessages() map { messages =>
        messages foreach { case Tuple2(mid, message) =>
          val pe = serializePackage(message)
          connector ! pe
        }
      }
    }
  }

  val receiveCommand: Receive = {
    case handleBox: HandleMessageBox =>
      withMDC(log.info(s"HandleMessageBox($handleBox)"))
      queueNewSession(handleBox.mb.messageId)
      context.become(receiveBusinessLogic)
      receiveBusinessLogic(handleBox)
    case p: AuthorizeUser =>
      receiveBusinessLogic(p)
    case m =>
      withMDC(log.error(s"received unmatched message: $m"))
  }

  val receiveBusinessLogic: Receive = {
    case handleBox: HandleMessageBox =>
      val connector = sender()
      checkNewConnection(connector)
      withMDC(log.info(s"HandleMessageBox ${handleBox.mb} $connector"))
      lastConnector = connector.some

      handleBox.mb.body match {
        case c@Container(_) => c.messages foreach (handleMessage(connector, _))
        case _ => handleMessage(connector, handleBox.mb)
      }
    case SendRpcResponseBox(connector, rpcBox) =>
      val mb = MessageBox(getMessageId(ResponseMsgId), rpcBox)
      withMDC(log.info(s"SendMessageBox $mb"))

      val origEncoded = serializeMessageBox(mb)
      val origLength = origEncoded.length / 8
      val blob = mb.body match {
        case RpcResponseBox(messageId, _) if origLength > maxResponseLength =>
          val unsentResponse = UnsentResponse(mb.messageId, messageId, origLength.toInt)
          //log.debug(s"$authId, $sessionId#Response is too large, generated $unsentResponse")
          serializeMessageBox(MessageBox(mb.messageId, unsentResponse))
        case _ => origEncoded
      }
      registerSentMessage(mb, blob)

      val pe = serializePackage(origEncoded)
      connector ! pe
    case UpdateBoxToSend(ub) =>
      withMDC(log.info(s"UpdateBoxToSend $ub"))
      val mb = MessageBox(getMessageId(UpdateMsgId), ub)
      val blob = serializeMessageBox(mb)

      if (!ub.body.isInstanceOf[WeakUpdate])
        registerSentMessage(mb, blob)

      val pe = serializePackage(blob)
      connectors foreach (_ ! pe)
    case msg @ AuthorizeUser(user) =>
      withMDC(log.debug(s"$msg"))
      if (!currentUser.exists(_ == user)) {
        persist(msg) { _ =>
          currentUser = Some(user)
          apiBroker ! ApiBrokerProtocol.AuthorizeUser(user)
        }
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
    // TODO: remove, deprecated case class
    case msg @ UnsubscribeToPresences(uids) =>
      persist(msg) { _ =>
        unsubscribeToPresences(uids)
      }
    case msg @ UnsubscribeFromPresences(uids) =>
      persist(msg) { _ =>
        unsubscribeToPresences(uids)
      }
    case msg @ SubscribeToGroupPresences(groupIds) =>
      persist(msg) { _ =>
        subscribeToGroupPresences(groupIds)
      }
    case msg @ UnsubscribeFromGroupPresences(groupIds) =>
      persist(msg) { _ =>
        unsubscribeFromGroupPresences(groupIds)
      }
    case SubscribeAck(ack) =>
      handleSubscribeAck(ack)
    case Terminated(connector) =>
      //log.debug(s"$authId, $sessionId#removing connector $connector")
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
      recoverSubscribeToGroupPresences(subscribedToPresencesGroupIds.toList)

      currentUser map { user =>
        apiBroker ! ApiBrokerProtocol.AuthorizeUser(user)
        context.become(receiveBusinessLogic)
      }
    case AuthorizeUser(user) =>
      currentUser = Some(user)
      apiBroker ! ApiBrokerProtocol.AuthorizeUser(user)
    case AuthorizeUserNew(user) =>
      currentUser = Some(user)
      apiBroker ! ApiBrokerProtocol.AuthorizeUser(user)
    case SubscribeToUpdates =>
      subscribingToUpdates = true
    case SubscribeToPresences(uids) =>
      subscribedToPresencesUids = subscribedToPresencesUids ++ uids
    // TODO: remove
    case UnsubscribeToPresences(uids) =>
      subscribedToPresencesUids = subscribedToPresencesUids -- uids
    case UnsubscribeFromPresences(uids) =>
      subscribedToPresencesUids = subscribedToPresencesUids -- uids
    case SubscribeToGroupPresences(groupIds) =>
      subscribedToPresencesGroupIds = subscribedToPresencesGroupIds ++ groupIds
    case UnsubscribeFromGroupPresences(groupIds) =>
      subscribedToPresencesGroupIds = subscribedToPresencesGroupIds -- groupIds
  }
}
