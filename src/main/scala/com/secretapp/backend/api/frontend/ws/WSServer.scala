package com.secretapp.backend.api.frontend.ws

import akka.actor._
import akka.io.IO
import spray.can.Http
import spray.can.websocket
import spray.can.websocket.frame.{ BinaryFrame, TextFrame }
import spray.http.HttpRequest
import spray.can.websocket.FrameCommandFailed
import spray.routing.HttpServiceActor
import com.datastax.driver.core.{ Session => CSession }

object WSServer {
  object WebSocketServer {
    def props(sessionRegion: ActorRef)(implicit session: CSession) = Props(new WebSocketServer(sessionRegion))
  }

  class WebSocketServer(sessionRegion: ActorRef)(implicit session: CSession) extends Actor with ActorLogging {
    def receive = {
      case Http.Connected(remoteAddress, localAddress) =>
        val connection = sender()
        val wsActor = context.actorOf(WSFrontend.props(connection, sessionRegion, session))
        connection ! Http.Register(wsActor)
    }
  }
}
