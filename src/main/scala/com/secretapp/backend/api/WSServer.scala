package com.secretapp.backend.api

import com.secretapp.backend.protocol.transport.WSConnector
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
      // when a new connection comes in we register a WebSocketConnection actor as the per connection handler
      case Http.Connected(remoteAddress, localAddress) =>
        val connection = sender()
        val wsActor = context.actorOf(WSConnector.props(connection, sessionRegion, session))
        connection ! Http.Register(wsActor)
    }
  }

}
