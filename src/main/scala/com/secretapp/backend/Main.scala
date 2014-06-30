package com.secretapp.backend

import java.net.InetSocketAddress
import akka.actor.{ ActorSystem, Props }
import akka.io.{ IO, Tcp }
import Tcp._
import api.Server

object Main extends App {

  implicit val system = ActorSystem("secret-api-server")

  implicit val service = system.actorOf(Props[Server], "api-service")

  IO(Tcp) ! Bind(service, new InetSocketAddress("0.0.0.0", 8080))

}
