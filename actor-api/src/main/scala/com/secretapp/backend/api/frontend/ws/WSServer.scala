package com.secretapp.backend.api.frontend.ws

import akka.actor._
import spray.can.Http
import com.datastax.driver.core.{ Session => CSession }

object WSServer {
  object WebSocketServer {
    def props(sessionRegion: ActorRef)(implicit session: CSession) = Props(new WebSocketServer(sessionRegion))
  }

  class WebSocketServer(sessionRegion: ActorRef)(implicit session: CSession) extends Actor with ActorLogging {

    override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

    override def postRestart(thr: Throwable): Unit = context stop self

    def receive = {
      case Http.Connected(remoteAddress, localAddress) =>
        val connection = sender()
        val wsActor = context.actorOf(WSFrontend.props(connection, remoteAddress, sessionRegion, session))
        connection ! Http.Register(wsActor)
    }
  }
}
