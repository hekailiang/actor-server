package com.secretapp.backend.protocol.transport

import akka.actor._
import akka.util.Timeout
import com.datastax.driver.core.{ Session => CSession }
import spray.can.websocket
import spray.can.websocket.frame.{ BinaryFrame, TextFrame }
import spray.http.HttpRequest
import spray.can.websocket.FrameCommandFailed
import spray.routing.HttpServiceActor

object WSConnector {
  def props(connection: ActorRef, sessionRegion: ActorRef, session: CSession) = {
    Props(new WSConnector(connection, sessionRegion, session))
  }
}

class WSConnector(val serverConnection: ActorRef, val sessionRegion: ActorRef, val session: CSession) extends HttpServiceActor with Connector with websocket.WebSocketServerWorker {
  import scala.concurrent.duration._

  implicit val timeout: Timeout = Timeout(5.seconds)

  override def handshaking: Receive = {
    case req: HttpRequest => // do something
      log.info(s"HttpRequest: $req, uri: ${req.uri.query}")
      val authId = req.uri.query.get("authId")
      val sessionId = req.uri.query.get("sessionId")
      log.info(s"sessionId: $sessionId, authId: $authId")

      context.become(super.handshaking orElse closeLogic)
      super.handshaking(req)
  }

  def businessLogic: Receive = {
//    case x @ (_: BinaryFrame | _: TextFrame) =>
    case x: TextFrame =>
      log.info(s"Frame: ${new String(x.payload.toArray)}")
      sender() ! x

    case x: FrameCommandFailed =>
      log.error("frame command failed", x)
  }

}
