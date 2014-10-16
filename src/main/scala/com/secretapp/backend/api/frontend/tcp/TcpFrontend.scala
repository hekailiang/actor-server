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
import java.net.InetSocketAddress

object TcpFrontend {
  def props(connection: ActorRef, remote: InetSocketAddress, sessionRegion: ActorRef, session: CSession) = {
    Props(new TcpFrontend(connection, remote, sessionRegion, session))
  }
}

class TcpFrontend(val connection: ActorRef, val remote: InetSocketAddress, val sessionRegion: ActorRef, val session: CSession) extends Frontend with NackActor with ActorLogging with MTPackageService {
  import akka.io.Tcp._

  val transport = MTConnection

  var packageIndex = 0

  context.setReceiveTimeout(15.minutes) // TODO

  def receiveBusinessLogic(writing: Boolean): Receive = {
    case Received(data) =>
      handleByteStream(BitVector(data.toArray))(handlePackage, e => sendDrop(e.msg))
    case ResponseToClient(payload) =>
      serialize2MTPackageBox(payload, writing)
    case ResponseToClientWithDrop(payload) =>
      serialize2MTPackageBox(payload, writing)
      silentClose()
    case SilentClose =>
      silentClose()
  }

  def serialize2MTPackageBox(payload: ByteString, writing: Boolean): Unit = {
    log.debug(s"packageIndex: $packageIndex, $payload")
    MTPackageBoxCodec.encode(packageIndex, BitVector(payload.toByteBuffer)) match {
      case \/-(reply) =>
        packageIndex += 1
        send(ByteString(reply.toByteBuffer), writing)
      case _ => silentClose()
    }
  }

  def silentClose(): Unit = {
    connection ! Close
    context.stop(self)
  }
}
