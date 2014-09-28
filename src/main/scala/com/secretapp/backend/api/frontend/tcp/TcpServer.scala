package com.secretapp.backend.api.frontend.tcp

import akka.actor._
import akka.io.Tcp._
import com.datastax.driver.core.{ Session => CSession }

object TcpServer {
  def props(sessionActor: ActorRef)(implicit csession: CSession) = Props(new TcpServer(sessionActor))
}

class TcpServer(sessionActor: ActorRef)(implicit csession: CSession) extends Actor with ActorLogging {
  import context.system

  def receive = ??? // {
//    case b @ Bound(localAddress) =>
//      log.info(s"Bound: $b")
//    case CommandFailed(_: Bind) =>
//      log.info("CommandFailed")
//      context stop self
//    case c @ Connected(remote, local) =>
//      log.info(s"Connected: $c")
//      val connection = sender()
//      val frontend = context.actorOf(TcpFrontend.props(connection, sessionActor))
//      connection ! Register(frontend)
//  }
}
