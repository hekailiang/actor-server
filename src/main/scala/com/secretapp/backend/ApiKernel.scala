package com.secretapp.backend

import java.net.InetSocketAddress
import akka.actor.{ ActorSystem, Props }
import akka.io.{ IO, Tcp }
import akka.kernel.Bootable
import Tcp._
import api.Server
import com.secretapp.backend.persist.DBConnector

class ApiKernel extends Bootable {

  implicit val system = ActorSystem("secret-api-server")

  def startup = {
    implicit val session = DBConnector.session
    implicit val service = system.actorOf(Props[Server], "api-service")
    IO(Tcp) ! Bind(service, new InetSocketAddress("0.0.0.0", 8080))
  }

  def shutdown = {
    system.shutdown()
  }
}
