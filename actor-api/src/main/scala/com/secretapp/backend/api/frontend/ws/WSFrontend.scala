package com.secretapp.backend.api.frontend.ws

import akka.actor._
import akka.util.ByteString
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.frontend._
import spray.can.websocket
import spray.can.websocket.frame._
import spray.can.websocket.FrameCommandFailed
import spray.routing.HttpServiceActor
import com.secretapp.backend.protocol.transport.{/*MTPackageBoxCodec, */MTPackageService, Frontend}
import com.secretapp.backend.protocol.codecs._
import scodec.bits.BitVector
import scalaz._
import Scalaz._
import java.net.InetSocketAddress

object WSFrontend {
  def props(connection: ActorRef, remote: InetSocketAddress, sessionRegion: ActorRef, session: CSession) = {
    Props(new WSFrontend(connection, remote, sessionRegion, session))
  }
}

// TODO: extend NackActor
class WSFrontend(val connection: ActorRef, val remote: InetSocketAddress, val sessionRegion: ActorRef, val session: CSession) extends HttpServiceActor with Frontend with websocket.WebSocketServerWorker with MTPackageService {
  val transport = MTConnection
  val serverConnection = connection
  // var packageIndex: Int = -1

  def businessLogic: Receive = {
    case frame: BinaryFrame =>
      log.info(s"Frame: ${BitVector(frame.payload).toHex}")
      // handleByteStream(BitVector(frame.payload))(handlePackage, e => sendDrop(e.msg))
      protoPackage.decode(BitVector(frame.payload)) match {
        case \/-((_, p)) =>
          handlePackage(p)
        case -\/(e) => sendDrop(e)
      }
    case frame: TextFrame =>
      log.error(s"TextFrame: ${new String(frame.payload.toArray)}")
    case x: FrameCommandFailed =>
      log.error(s"frame command failed: $x")
    case ResponseToClient(bs) =>
      log.info(s"ResponseToClient: $bs")
      send(BinaryFrame(bs))
    case ResponseToClientWithDrop(bs) =>
      send(BinaryFrame(bs))
      silentClose("ResponseToClientWithDrop")
    case SilentClose =>
      silentClose("SilentClose")
  }

  // def serialize2MTPackageBox(payload: ByteString): Unit = {
  //   packageIndex += 1
  //   MTPackageBoxCodec.encode(packageIndex, BitVector(payload.toByteBuffer)) match {
  //     case \/-(reply) => send(BinaryFrame(ByteString(reply.toByteBuffer)))
  //     case -\/(e) => silentClose(e)
  //   }
  // }

  def silentClose(reason: String): Unit = {
    log.error(s"WSFrontend.silentClose: $reason")
    send(CloseFrame())
    context.stop(self)
  }
}
