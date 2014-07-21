package com.secretapp.backend

import java.net.InetSocketAddress
import akka.actor.{ ActorSystem, Props }
import akka.io.{ IO, Tcp }
import akka.kernel.Bootable
import Tcp._
import scala.util.Try
import com.secretapp.backend.persist.DBConnector
import api.Server
import com.typesafe.config._

class ApiKernel extends Bootable with ExtraActorsInitializer {
  implicit val serverConfig = ConfigFactory.load().getConfig("secret.server")

  implicit val system = ActorSystem("secret-api-server")

  def startup = {
    val port = Try(serverConfig.getInt("port")).getOrElse(8080)
    val hostname = Try(serverConfig.getString("hostname")).getOrElse("0.0.0.0")
    val session = DBConnector.session
    DBConnector.createTables(session)
    implicit val service = system.actorOf(Props(new Server(session)), "api-service")
    initExtraActors(system)
    IO(Tcp) ! Bind(service, new InetSocketAddress(hostname, port))
  }

  def shutdown = {
    system.shutdown()
  }
}
