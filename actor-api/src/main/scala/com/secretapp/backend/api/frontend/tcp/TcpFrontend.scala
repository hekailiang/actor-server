package com.secretapp.backend.api.frontend.tcp

import akka.actor._
import akka.util.ByteString
import com.secretapp.backend.api.frontend._
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.data.message.Drop
import com.secretapp.backend.protocol.transport.{MTPackageBoxCodec, MTPackageService, Frontend}
import scodec.bits.BitVector
import scala.concurrent.duration._
import scalaz._
import Scalaz._
import java.net.InetSocketAddress

object TcpFrontend {
  def props(connection: ActorRef, remote: InetSocketAddress, sessionRegion: ActorRef) = {
    Props(new TcpFrontend(connection, remote, sessionRegion))
  }
}

class TcpFrontend(val connection: ActorRef, val remote: InetSocketAddress, val sessionRegion: ActorRef) extends Frontend with NackActor with ActorLogging with MTPackageService {
  import akka.io.Tcp._

  val transport = MTConnection

  var packageIndex: Int = -1

  context.setReceiveTimeout(15.minutes) // TODO

  def receiveBusinessLogic(writing: Boolean): Receive = {
    case Received(data) =>
      //log.debug(s"$authId#Received($data)")
      handleByteStream(BitVector(data.toArray))(handlePackage, e => sendDrop(e.msg))
    case ResponseToClient(payload) =>
      //log.debug(s"$authId#ResponseToClient($payload)")
      serialize2MTPackageBox(payload, writing)
    case ResponseToClientWithDrop(payload) =>
      serialize2MTPackageBox(payload, writing)
      silentClose(s"$authId#ResponseToClientWithDrop")
    case SilentClose =>
      silentClose("SilentClose")
    case ReceiveTimeout =>
      silentClose("ReceiveTimeout")
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
    // TODO
    val pkg = transport.buildPackage(0L, 0, MessageBox(0, Drop(0, reason)))
    connection ! Write(pkg.encode)
    connection ! Close
  }
}
