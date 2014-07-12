package com.secretapp.backend

import java.net.InetSocketAddress
import akka.actor.{ ActorSystem, Props }
import akka.io.{ IO, Tcp }
import akka.kernel.Bootable
import Tcp._
import api.Server
import com.secretapp.backend.persist.DBConnector
import com.typesafe.config._

class ApiKernel extends Bootable {
  val serverConfig = ConfigFactory.load().getConfig("secret.server")

  implicit val system = ActorSystem("secret-api-server")

  def startup = {
    val session = DBConnector.session
    implicit val service = system.actorOf(Props(new Server(session)), "api-service")
    IO(Tcp) ! Bind(service, new InetSocketAddress("0.0.0.0", serverConfig.getInt("port")))
  }

  def shutdown = {
    system.shutdown()
  }
}
