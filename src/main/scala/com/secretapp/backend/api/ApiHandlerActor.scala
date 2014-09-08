package com.secretapp.backend.api

import akka.actor.{ Actor, ActorRef, ActorLogging }
import akka.util.ByteString
import com.secretapp.backend.data.transport.MessageBox
import scodec.bits._
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.services.transport._
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data._
import PackageCommon._
import scalaz._
import Scalaz._
import com.datastax.driver.core.{ Session => CSession }

// TODO: replace connection: ActorRef hack with real sender (or forget it?)
class ApiHandlerActor(connection: ActorRef, val clusterProxies: ClusterProxies)(implicit val session: CSession) extends Actor with ActorLogging
    with WrappedPackageService with PackageService {
  import akka.io.Tcp._
  case class Ack(stamp: Int) extends Event
  var packageIndex = 0

  val handleActor = self

  context watch connection

  def receive = {
    case PackageToSend(pe) =>
      log.info(s"PackageToSend($pe)")
      pe match {
        case \/-(p) =>
          write(connection, replyPackage(packageIndex, p))
        case -\/(p) =>
          write(connection, replyPackage(packageIndex, p))
          connection ! Close
        case x =>
          log.error(s"unhandled packageToSend ${x}")
      }

    case MessageBoxToSend(mb) =>
      log.info(s"MessageBoxToSend($mb)")
      val p = Package(getAuthId, getSessionId, mb)
      write(connection, replyPackage(packageIndex, p))

    case UpdateBoxToSend(ub) =>
      log.info(s"UpdateBoxToSend($ub)")
      // FIXME: real message id SA-32
      val p = Package(getAuthId, getSessionId, MessageBox(rand.nextLong, ub))
      write(connection, replyPackage(packageIndex, p))

    case m: ServiceMessage =>
      log.info(s"ServiceMessage: $m")
      serviceMessagesPF(m)

    case Received(data) =>
      log.info(s"Received: $data ${data.length}")
      handleByteStream(BitVector(data.toArray))(handlePackage, handleError)

    case PeerClosed =>
      log.info("Connection closed by peer")
      context stop self

    case ErrorClosed(msg) =>
      log.error(s"ErrorClosed ${msg}")
      context stop self

    case CommandFailed(x) =>
      log.error(s"CommandFailed ${x}")

    case Closed =>
      log.info(s"Connection closed by listener")
      context stop self

    case Ack(index) =>
      log.info(s"Ack ${index}")
  }

  def write(connection: ActorRef, byteString: ByteString): Unit = {
    log.debug(s"Sending ${connection} ${byteString} ${packageIndex}")
    connection ! Write(byteString, Ack(packageIndex))
    packageIndex += 1
  }
}
