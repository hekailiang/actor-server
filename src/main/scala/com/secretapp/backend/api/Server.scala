package com.secretapp.backend.api

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.io.Tcp._
import com.datastax.driver.core.{ Session => CSession }
import com.notnoop.apns.APNS
import com.secretapp.backend.api.counters.FilesCounter
import com.secretapp.backend.services.rpc.typing.TypingBroker
import com.secretapp.backend.services.rpc.presence.{ GroupPresenceBroker, PresenceBroker }
import com.secretapp.backend.session._
import com.secretapp.backend.protocol.transport._
import com.secretapp.backend.sms.ClickatellSmsEngineActor
import com.typesafe.config.ConfigFactory

final class Singletons(implicit val system: ActorSystem, session: CSession) {
  val config = ConfigFactory.load()
  val appConfig = config.getConfig("secret")
  val filesCounter = FilesCounter.start(system)
  val smsEngine = ClickatellSmsEngineActor()
  val typingBrokerRegion = TypingBroker.startRegion()
  val presenceBrokerRegion = PresenceBroker.startRegion()
  val groupPresenceBrokerRegion = GroupPresenceBroker.startRegion()
  val apnsService = APNS.newService.withCert(
    appConfig.getString("apns.cert.path"),
    appConfig.getString("apns.cert.password")
  ).withProductionDestination.build
}

final class ClusterProxies(implicit val system: ActorSystem) {
  val filesCounter = FilesCounter.startProxy(system)
}

class Server(implicit session: CSession) extends Actor with ActorLogging {
  import context.system

  implicit val clusterProxies = new ClusterProxies
  implicit val singletons = new Singletons

  val sessionRegion = SessionActor.startRegion()

  def receive = {
    case b @ Bound(localAddress) =>
      log.info(s"Bound: $b")
    case CommandFailed(_: Bind) =>
      log.info("CommandFailed")
      context stop self
    case c @ Connected(remote, local) =>
      log.info(s"Connected: $c")
      val connection = sender()
      val frontendActor = context.actorOf(Props(new TcpConnector(connection, sessionRegion, session)))
      connection ! Register(frontendActor, keepOpenOnPeerClosed = true)
  }
}
