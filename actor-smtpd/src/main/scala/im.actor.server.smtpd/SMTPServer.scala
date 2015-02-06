package im.actor.server.smtpd

import akka.actor._
import akka.io._
import java.net._
import javax.net.ssl.{KeyManagerFactory, TrustManagerFactory}
import com.secretapp.backend.api.{Singletons, SocialBroker, UpdatesBroker}
import akka.routing.{DefaultResizer, RoundRobinPool}
import com.secretapp.backend.api.counters.EmailsCounter
import im.actor.server.smtpd.internal.ApiBrokerActor
import com.typesafe.config._
import scala.util.Try

object SMTPServer {
  val config = ConfigFactory.load().getConfig("actor-server.smtpd") // TODO: move into start arguments

  val hostname = InetAddress.getLocalHost.getHostName
  val mailHost = config.getString("hostname")

  def start(singletons: Singletons, keyManagerFactory: KeyManagerFactory, trustManagerFactory: TrustManagerFactory)(implicit system: ActorSystem): Unit = {
    val updatesBrokerRegion = UpdatesBroker.startRegion(singletons.apnsService)(system)
    val socialBrokerRegion = SocialBroker.startRegion()

    EmailsCounter.start
    val emailCounter = EmailsCounter.startProxy

    val resizer = DefaultResizer(lowerBound = 2, upperBound = 15)

    val apiRouter = system.actorOf(RoundRobinPool(10, Some(resizer))
      .props(ApiBrokerActor.props(singletons, updatesBrokerRegion, socialBrokerRegion)), "api-broker-router")

    val mailRouter = system.actorOf(RoundRobinPool(10, Some(resizer))
      .props(MailActor.props(emailCounter, apiRouter)), "mail-router")

    val interface = Try(config.getString("interface")).getOrElse("0.0.0.0")
    val port = Try(config.getInt("port")).getOrElse(1025)
    val tlsPort = Try(config.getInt("tls-port")).getOrElse(10465)

    system.actorOf(Props(new SMTPServer(interface, port, hostname, mailRouter, keyManagerFactory, trustManagerFactory, tlsConnection = false)))
    system.actorOf(Props(new SMTPServer(interface, tlsPort, hostname, mailRouter, keyManagerFactory, trustManagerFactory, tlsConnection = true)))
  }
}

class SMTPServer(interface: String, port: Int, hostname: String, mailRouter: ActorRef, keyManagerFactory: KeyManagerFactory, trustManagerFactory: TrustManagerFactory, tlsConnection: Boolean) extends Actor with ActorLogging {
  import akka.io.Tcp._
  import context.system

  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

  override def preStart(): Unit = {
    IO(Tcp) ! Bind(self, new InetSocketAddress(interface, port))
  }

  override def postRestart(thr: Throwable): Unit = context.stop(self)

  def receive = {
    case CommandFailed(_: Bind) =>
      context.stop(self)
    case c @ Connected(remote, local) =>
      log.debug(s"Connected: $c")
      val connection = sender()
      val frontend = context.actorOf(SMTPFrontend.props(connection, remote, hostname, mailRouter, keyManagerFactory, trustManagerFactory, tlsConnection))
      connection ! Register(frontend, keepOpenOnPeerClosed = true)
  }
}
