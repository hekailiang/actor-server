package com.secretapp.backend.api.frontend.ws

import akka.actor._
import spray.can.Http
import spray.can.websocket
import spray.can.websocket.frame.{ BinaryFrame, TextFrame }
import spray.http.HttpRequest
import spray.can.websocket.FrameCommandFailed
import spray.routing.HttpServiceActor
import com.datastax.driver.core.{ Session => CSession }
import java.net.InetSocketAddress

object WSServer {
  object WebSocketServer {
    def props(sessionRegion: ActorRef)(implicit session: CSession) = Props(new WebSocketServer(sessionRegion))
  }

  class WebSocketServer(sessionRegion: ActorRef)(implicit session: CSession) extends Actor with ActorLogging {
    import akka.io.Tcp._

    override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

//    override def preStart(): Unit = {
//      IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 0))
//    }

    override def postRestart(thr: Throwable): Unit = context stop self

    def receive = {
      case Http.Connected(remoteAddress, localAddress) =>
        val connection = sender()
        val wsActor = context.actorOf(WSFrontend.props(connection, remoteAddress, sessionRegion, session))
        connection ! Http.Register(wsActor)
    }
  }
}
