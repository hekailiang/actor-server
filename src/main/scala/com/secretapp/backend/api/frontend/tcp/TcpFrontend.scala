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
      log.debug(s"ResponseToClient($payload)")
      serialize2MTPackageBox(payload, writing)
    case ResponseToClientWithDrop(payload) =>
      serialize2MTPackageBox(payload, writing)
      silentClose("ResponseToClientWithDrop")
    case SilentClose =>
      silentClose("SilentClose")
  }

  def serialize2MTPackageBox(payload: ByteString, writing: Boolean): Unit = {
    MTPackageBoxCodec.encode(getPackageIndex, BitVector(payload.toByteBuffer)) match {
      case \/-(reply) =>
        send(ByteString(reply.toByteBuffer), writing)
      case -\/(e) => silentClose(e)
    }
  }

  private def getPackageIndex: Int = {
    val res = packageIndex
    log.debug(s"packageIndex: $res, ${self.path.name}")
    packageIndex += 1
    res
  }

  def silentClose(reason: String): Unit = {
    log.error(s"TcpFrontend.silentClose: $reason")
    connection ! Close
    context.stop(self)
  }
}
