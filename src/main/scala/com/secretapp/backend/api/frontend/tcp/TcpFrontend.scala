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

class TcpFrontend(val connection: ActorRef, val remote: InetSocketAddress, val sessionRegion: ActorRef, val session: CSession) extends Frontend with NackActor with ActorLogging with MTPackageService with SslConfiguration {
  import akka.io.Tcp._

  val transport = MTConnection

  var packageIndex: Int = -1

  context.setReceiveTimeout(15.minutes) // TODO

  def receiveBusinessLogic(writing: Boolean): Receive = {
    case Received(data) =>
      log.debug(s"$authId#Received($data)")
      handleByteStream(BitVector(data.toArray))(handlePackage, e => sendDrop(e.msg))
    case ResponseToClient(payload) =>
      log.debug(s"$authId#ResponseToClient($payload)")
      serialize2MTPackageBox(payload, writing)
    case ResponseToClientWithDrop(payload) =>
      serialize2MTPackageBox(payload, writing)
      silentClose(s"$authId#ResponseToClientWithDrop")
    case SilentClose =>
      silentClose("SilentClose")
  }

  def serialize2MTPackageBox(payload: ByteString, writing: Boolean): Unit = {
    packageIndex += 1
    MTPackageBoxCodec.encode(packageIndex, BitVector(payload.toByteBuffer)) match {
      case \/-(reply) => send(ByteString(reply.toByteBuffer), writing)
      case -\/(e) => silentClose(e)
    }
  }

  def silentClose(reason: String): Unit = {
    log.error(s"$authId#TcpFrontend.silentClose: $reason")
    connection ! Close
    context.stop(self)
  }
}
