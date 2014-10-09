package com.secretapp.backend

import akka.cluster.Cluster
import java.net.InetSocketAddress
import akka.actor.{ ActorSystem, Props }
import akka.io.{ IO, Tcp }
import akka.kernel.Bootable
import com.secretapp.backend.api._
import com.secretapp.backend.api.frontend.tcp.TcpServer
import com.secretapp.backend.api.frontend.ws.WSServer
import com.secretapp.backend.session.SessionActor
import spray.can.Http
import spray.can.server.UHttp
import scala.util.Try
import com.secretapp.backend.persist.DBConnector

class ApiKernel extends Bootable {
  import Tcp._

  implicit val system = ActorSystem("secret-api-server")

  val joinAddress = Cluster(system).selfAddress
  Cluster(system).join(joinAddress)

  import system.dispatcher

  import Configuration._

  def startup = {
    // C* initialize
    implicit val session = DBConnector.session
    DBConnector.createTables(session)

    // Session bootstrap
    val clusterProxies = new ClusterProxies
    val singletons = new Singletons
    val sessionRegion = SessionActor.startRegion()(system, singletons, clusterProxies, session)

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
  }

  def shutdown = {
    system.shutdown()
  }
}
