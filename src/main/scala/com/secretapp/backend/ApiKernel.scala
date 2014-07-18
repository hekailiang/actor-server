package com.secretapp.backend

import java.net.InetSocketAddress
import akka.actor.{ ActorSystem, Props }
import akka.io.{ IO, Tcp }
import akka.kernel.Bootable
import Tcp._
import scala.util.Try
import api.Server
import com.secretapp.backend.persist.DBConnector
import com.typesafe.config._

class ApiKernel extends Bootable {
  val serverConfig = ConfigFactory.load().getConfig("secret.server")

  implicit val system = ActorSystem("secret-api-server")

  def startup = {
    val session = DBConnector.session
    DBConnector.createTables(session)
    implicit val service = system.actorOf(Props(new Server(session)), "api-service")
    val port = Try(serverConfig.getInt("port")).getOrElse(8080)
    val hostname = Try(serverConfig.getString("hostname")).getOrElse("0.0.0.0")
    IO(Tcp) ! Bind(service, new InetSocketAddress(hostname, port))
  }

  def shutdown = {
    system.shutdown()
  }
}
