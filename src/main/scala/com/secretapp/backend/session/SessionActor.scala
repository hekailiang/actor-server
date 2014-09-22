package com.secretapp.backend.session

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.SubscribeAck
import akka.contrib.pattern.ClusterSharding
import akka.contrib.pattern.ShardRegion
import akka.persistence._
import com.secretapp.backend.data.message.Container
import com.secretapp.backend.data.message.ResponseAuthId
import com.secretapp.backend.data.message.RpcRequestBox
import com.secretapp.backend.data.models.User
import com.secretapp.backend.services.SessionManager
import com.secretapp.backend.services.common.RandomService
import scala.concurrent.duration._
import akka.util.{ ByteString, Timeout }
import scala.collection.immutable
import com.secretapp.backend.data.transport.MessageBox
import com.datastax.driver.core.{ Session => CSession }
import scodec.bits._
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.protocol.transport._
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.api._
import com.secretapp.backend.api.rpc._
import com.secretapp.backend.data.message.{MessageAck, Pong, Ping}
import com.secretapp.backend.data._
import PackageCommon._
import scalaz._
import Scalaz._
import com.datastax.driver.core.{ Session => CSession }

object SessionProtocol {
  // TODO: wrap all messages into Envelope
  sealed trait SessionMessage
  case class NewConnection(connector: ActorRef) extends SessionMessage
  case class HandlePackage(p: Package) extends SessionMessage
  case class AuthorizeUser(user: User) extends SessionMessage
  case object SubscribeToUpdates extends SessionMessage
  case class Envelope(authId: Long, sessionId: Long, payload: SessionMessage)
}

object SessionActor {
  import SessionProtocol._

  private val idExtractor: ShardRegion.IdExtractor = {
    case Envelope(authId, sessionId, msg) => (s"${authId}_${sessionId}", msg)
    case msg @ HandlePackage(Package(authId, sessionId, _)) => (s"${authId}_${sessionId}", msg)
  }

  private val shardCount = 2 // TODO: configurable

  private val shardResolver: ShardRegion.ShardResolver = {
    // TODO: better balancing
    case Envelope(authId, sessionId, msg) => (authId * sessionId % shardCount).toString
    case msg @ HandlePackage(Package(authId, sessionId, _)) => (authId * sessionId % shardCount).toString
  }

  def startRegion()(implicit system: ActorSystem, clusterProxies: ClusterProxies, session: CSession) = ClusterSharding(system).start(
    typeName = "Session",
    entryProps = Some(Props(new SessionActor(clusterProxies, session))),
    idExtractor = idExtractor,
    shardResolver = shardResolver
  )

}

class SessionActor(clusterProxies: ClusterProxies, session: CSession) extends SessionService with PersistentActor with PackageAckService with RandomService with ActorLogging {
  import ShardRegion.Passivate
  import SessionProtocol._
  import AckTrackerProtocol._
  case object Stop

  context.setReceiveTimeout(15.minutes)

  implicit val timeout = Timeout(5.seconds)

  override def persistenceId: String = self.path.parent.name + "-" + self.path.name

  val splitName = self.path.name.split("_")
  val authId = java.lang.Long.parseLong(splitName(0))
  val sessionId = java.lang.Long.parseLong(splitName(1))

  val mediator = DistributedPubSubExtension(context.system).mediator
  val updatesPusher = context.actorOf(Props(new PusherActor(context.self, authId)))

  var connectors = immutable.Set.empty[ActorRef]
  var lastConnector: Option[ActorRef] = None

  // we need lazy here because subscribedToUpdates sets during receiveRecover
  lazy val apiBroker = context.actorOf(Props(new ApiBrokerActor(authId, sessionId, clusterProxies, subscribedToUpdates, session)), "api-broker")

  val receiveCommand: Receive = {
    case NewConnection(connector) =>
      log.debug(s"NewConnection ${connector}")
      connectors = connectors + connector
      lastConnector = Some(connector)
      context.watch(connector)
    case HandlePackage(p) =>
      val connector = sender()
      log.debug(s"HandlePackage $p $connector")
      lastConnector = Some(connector)

      p.messageBox.body match {
        case c@Container(_) => c.messages foreach (handleMessage(connector, p, _))
        case _ => handleMessage(connector, p, p.messageBox)
      }
    case PackageToSend(connector, pe) =>
      log.debug(s"PackageToSend ${connector} ${pe}")
      // TODO: persist
      connector ! pe
    case UpdateBoxToSend(ub) =>
      log.debug(s"UpdateBoxToSend($ub)")
      // FIXME: real message id SA-32
      val pe = Package(authId, sessionId, MessageBox(rand.nextLong, ub)).right
      connectors foreach (_ ! pe)
    case AuthorizeUser(user) =>
      apiBroker ! ApiBrokerProtocol.AuthorizeUser(user)
    case msg @ SubscribeToUpdates =>
      if (!subscribedToUpdates && !subscribingToUpdates) {
        persist(msg) { _ =>
          subscribeToUpdates()
        }
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
    case SubscribeToUpdates =>
      subscribingToUpdates = true
    case _ =>
  }
}
