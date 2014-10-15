package com.secretapp.backend.api.frontend.tcp

import akka.actor._
import akka.util.ByteString
import com.secretapp.backend.api.frontend._
import com.secretapp.backend.protocol.transport.{MTPackageBoxCodec, MTPackageService, Frontend}
import scodec.bits.BitVector
import scala.concurrent.duration._
import scalaz._
import Scalaz._
import com.datastax.driver.core.{ Session => CSession }

object TcpFrontend {
  def props(connection: ActorRef, sessionRegion: ActorRef, session: CSession) = {
    Props(new TcpFrontend(connection, sessionRegion, session))
  }
}

class TcpFrontend(val connection: ActorRef, val sessionRegion: ActorRef, val session: CSession) extends Frontend with ActorLogging with MTPackageService {
  import akka.io.Tcp._

  val transport = MTConnection

  var packageIndex = 0

  context.watch(connection)
  context.setReceiveTimeout(15.minutes) // TODO

  def receive = {
    case Received(data) =>
      handleByteStream(BitVector(data.toArray))(handlePackage, e => sendDrop(e.msg))
    case ResponseToClient(payload) =>
      log.info(s"Send to client: $payload")
      write(payload)
    case ResponseToClientWithDrop(payload) =>
      write(payload)
      silentClose()
    case SilentClose =>
      silentClose()
    case PeerClosed | ErrorClosed | Closed =>
      log.info(s"Connection closed")
      context stop self
    case CommandFailed(Write(data, _)) =>
      connection ! Write(data)
  }

  def write(payload: ByteString): Unit = {
    log.debug(s"packageIndex: $packageIndex, $payload")
    MTPackageBoxCodec.encode(packageIndex, BitVector(payload.toByteBuffer)) match {
      case \/-(reply) =>
        packageIndex += 1
        connection ! Write(ByteString(reply.toByteBuffer))
      case _ => silentClose()
    }
  }

  def silentClose(): Unit = {
    connection ! Close
    context.stop(self)
  }
}
