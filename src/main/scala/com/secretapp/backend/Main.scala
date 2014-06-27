package com.secretapp.backend

import java.net.InetSocketAddress
import akka.actor.{ ActorSystem, Props }
import akka.io.{ IO, Tcp }
import protocol.types._

object Main extends App {

  import Tcp._

  implicit val system = ActorSystem("secret-api-server")

  implicit val service = system.actorOf(Props[Server], "api-service")

  IO(Tcp) ! Bind(service, new InetSocketAddress("0.0.0.0", 8080))

//  import protocol._
//  import protocol.types._
//  val m = ResponseAuth(34L)
//  val p = Package.build(0L, 0L, 1L, m)
//  println(s"Package.encode(p): ${p}")

}
