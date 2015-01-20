package im.actor.backend

import akka.cluster.Cluster
import akka.actor.{ ActorSystem, Props }
import akka.io.{ IO, Tcp }
import akka.kernel.Bootable
import com.secretapp.backend.api._
import com.secretapp.backend.api.frontend.tcp.TcpServer
import com.secretapp.backend.api.frontend.ws.WSServer
import com.secretapp.backend.session.SessionActor
import com.secretapp.backend.persist
import im.actor.server.smtpd.SMTPServer
import com.typesafe.config._
import java.net.InetSocketAddress
import im.actor.server.persist.{ FlywayInit, DbInit }
import im.actor.server.persist.file.adapter.fs.FileStorageAdapter
import scala.concurrent.duration._
import spray.can.Http
import spray.can.server.UHttp
import scala.util.Try

class ApiKernel extends Bootable with FlywayInit with DbInit {
  import Tcp._

  val config = ConfigFactory.load()

  val serverConfig = config.getConfig("actor-server")
  val sqlConfig = serverConfig.getConfig("sql")

  val flyway = initFlyway(sqlConfig)

  flyway.migrate()

  initDb(sqlConfig)

  implicit val system = ActorSystem(serverConfig.getString("actor-system-name"), serverConfig)

  import system.dispatcher

  def startup() = {
    Cluster(system)

    // Session bootstrap
    val singletons = new Singletons

    val fileAdapter = new FileStorageAdapter(system)

    val sessionReceiveTimeout = system.settings.config.getDuration("session.receive-timeout", MILLISECONDS)
    val sessionRegion = SessionActor.startRegion(singletons, fileAdapter, sessionReceiveTimeout.milliseconds)(system)

    val serverConfig = system.settings.config.getConfig("server")

    // TCP transport bootstrap
    val tcpPort = Try(serverConfig.getInt("tcp-port")).getOrElse(8080)
    val hostname = Try(serverConfig.getString("hostname")).getOrElse("0.0.0.0")
    val tcpService = system.actorOf(TcpServer.props(sessionRegion), "api-service")
    val address = new InetSocketAddress(hostname, tcpPort)
    IO(Tcp) ! Bind(tcpService, address)

    // Heating TCP actors
    system.actorOf(Props(new MTHeatingUpActor(address)), "mt-heat-service")

    // WS transport bootstrap
    val wsPort = Try(serverConfig.getInt("ws-port")).getOrElse(8082)
    val wsService = system.actorOf(WSServer.WebSocketServer.props(sessionRegion), "ws-service")
    IO(UHttp) ! Http.Bind(wsService, hostname, wsPort)

    // Heating WS actors
    system.actorOf(Props(new WSHeatingUpActor(hostname, wsPort)), "ws-heat-service")

    // SMTP service
    SMTPServer.start(singletons)
  }

  def shutdown() = {
    system.shutdown()
  }
}
