package com.secretapp.backend.api.frontend.ws

import akka.actor._
import akka.util.ByteString
import akka.util.Timeout
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.frontend._
import scodec.bits.BitVector
import spray.can.websocket
import spray.can.websocket.frame._
import spray.can.websocket.Send
import spray.http.HttpRequest
import spray.can.websocket.FrameCommandFailed
import spray.routing.HttpServiceActor
import com.secretapp.backend.protocol.transport.{JsonPackageCodec, Frontend}
import scalaz._
import Scalaz._
import java.net.InetSocketAddress

object WSFrontend {
  def props(connection: ActorRef, remote: InetSocketAddress, sessionRegion: ActorRef, session: CSession) = {
    Props(new WSFrontend(connection, remote, sessionRegion, session))
  }
}

class WSFrontend(val connection: ActorRef, val remote: InetSocketAddress, val sessionRegion: ActorRef, val session: CSession) extends HttpServiceActor with Frontend with websocket.WebSocketServerWorker with SslConfiguration {
  val transport = JsonConnection
  val serverConnection = connection

  def businessLogic: Receive = {
    case frame: TextFrame =>
      log.info(s"Frame: ${new String(frame.payload.toArray)}")
      JsonPackageCodec.decode(frame.payload) match {
        case \/-(p) => handlePackage(p)
        case -\/(e) => sendDrop(e)
      }
    case x: FrameCommandFailed =>
      log.error(s"frame command failed: $x")
    case ResponseToClient(bs) =>
      log.info(s"ResponseToClient: $bs")
      send(TextFrame(bs))
    case ResponseToClientWithDrop(bs) =>
      send(TextFrame(bs))
      silentClose("ResponseToClientWithDrop")
    case SilentClose =>
      silentClose("SilentClose")
  }

  def silentClose(reason: String): Unit = {
    log.error(s"WSFrontend.silentClose: $reason")
    send(CloseFrame())
    context.stop(self)
  }
}
