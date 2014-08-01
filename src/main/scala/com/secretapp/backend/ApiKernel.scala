package com.secretapp.backend

import akka.cluster.Cluster
import java.net.InetSocketAddress
import akka.actor.{ ActorSystem, Props }
import akka.io.{ IO, Tcp }
import akka.kernel.Bootable
import Tcp._
import scala.util.Try
import com.secretapp.backend.persist.DBConnector
import api.{ Server, HeatingUpActor }
import com.secretapp.backend.api.counters._

class ApiKernel extends Bootable {
  implicit val system = ActorSystem("secret-api-server")

  val joinAddress = Cluster(system).selfAddress
  Cluster(system).join(joinAddress)

  import system.dispatcher

  import Configuration._

  def startup = {
    val port = Try(serverConfig.getInt("port")).getOrElse(8080)
    val hostname = Try(serverConfig.getString("hostname")).getOrElse("0.0.0.0")
    val session = DBConnector.session
    DBConnector.createTables(session)
    implicit val service = system.actorOf(Props(new Server(session)), "api-service")
    val address = new InetSocketAddress(hostname, port)
    IO(Tcp) ! Bind(service, address)
    system.actorOf(Props(new HeatingUpActor(address)), "heat-service")

    FilesCounter.start(system)

  }

  def shutdown = {
    system.shutdown()
  }
}
