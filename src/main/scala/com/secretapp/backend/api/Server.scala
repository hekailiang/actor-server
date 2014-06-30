package com.secretapp.backend.api

import akka.actor.{ Actor, ActorLogging, Props }
import akka.io.Tcp._
import java.util.concurrent.ConcurrentHashMap

class Server extends Actor with ActorLogging {

  import context.system

  private val authTable = new ConcurrentHashMap[Long, Long]()

  def receive = {
    case b @ Bound(localAddress) =>
      log.info(s"Bound: $b")
    case CommandFailed(_: Bind) =>
      log.info("CommandFailed")
      context stop self
    case c @ Connected(remote, local) =>
      log.info(s"Connected: $c")
      val handler = context.actorOf(Props(classOf[ApiHandler], authTable))
      val connection = sender()
      connection ! Register(handler)
  }
}

