package com.secretapp.backend

import akka.cluster.Cluster
import java.net.InetSocketAddress
import akka.actor.{ ActorSystem, Props }
import akka.io.{ IO, Tcp }
import akka.kernel.Bootable
import Tcp._
import com.secretapp.backend.api.counters.FilesCounter
import com.secretapp.backend.api._
import com.secretapp.backend.services.rpc.presence.PresenceBroker
import com.secretapp.backend.session.SessionActor
import spray.can.Http
import spray.can.server.UHttp
import scala.util.Try
import com.secretapp.backend.persist.DBConnector

class ApiKernel extends Bootable {
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
    val sessionRegion = SessionActor.startRegion()(system, clusterProxies, session)

    // TCP transport bootstrap
    val tcpPort = Try(serverConfig.getInt("tcp-port")).getOrElse(8080)
    val hostname = Try(serverConfig.getString("hostname")).getOrElse("0.0.0.0")
    val tcpService = system.actorOf(Server.props(sessionRegion), "api-service")
    val address = new InetSocketAddress(hostname, tcpPort)
    IO(Tcp) ! Bind(tcpService, address)

    // Heating TCP actors
    system.actorOf(Props(new HeatingUpActor(address)), "heat-service")

    // WS transport bootstrap
    val wsPort = Try(serverConfig.getInt("ws-port")).getOrElse(8081)
    val wsService = system.actorOf(WSServer.WebSocketServer.props(sessionRegion), "ws-service")
    IO(UHttp) ! Http.Bind(wsService, hostname, wsPort)

    // TODO: Heating WS actors
  }

  def shutdown = {
    system.shutdown()
  }
}
