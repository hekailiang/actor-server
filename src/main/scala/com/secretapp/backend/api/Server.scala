package com.secretapp.backend.api

import akka.actor.{ Actor, ActorLogging, Props }
import akka.io.Tcp._
import com.datastax.driver.core.{ Session => CSession }

class Server(session: CSession) extends Actor with ActorLogging {

  import context.system

  def receive = {
    case b @ Bound(localAddress) =>
      log.info(s"Bound: $b")
    case CommandFailed(_: Bind) =>
      log.info("CommandFailed")
      context stop self
    case c @ Connected(remote, local) =>
      log.info(s"Connected: $c")
      val connection = sender()
      val handler = context.actorOf(Props(new ApiHandlerActor(connection, session)))
      connection ! Register(handler)
  }
}
