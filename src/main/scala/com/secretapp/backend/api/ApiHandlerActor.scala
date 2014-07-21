package com.secretapp.backend.api

import akka.actor.{ Actor, ActorRef, ActorLogging }
import akka.util.ByteString
import akka.io.Tcp._
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
class ApiHandlerActor(connection: ActorRef, val session: CSession) extends Actor with ActorLogging
    with WrappedPackageService with PackageService {
  val handleActor = self

  def receive = {
    case PackageToSend(pe) => log.info(s"PackageToSend($pe)"); pe match {
      case \/-(p) =>
        connection ! Write(replyPackage(p))
      case -\/(p) =>
        connection ! Write(replyPackage(p))
        connection ! Close
    }

    case MessageBoxToSend(mb) =>
      log.info(s"MessageBoxToSend($mb)")
      val p = Package(getAuthId, getSessionId, mb)
      connection ! Write(replyPackage(p))

    case m: ServiceMessage =>
      log.info(s"ServiceMessage: $m")
      serviceMessagesPF(m)


    case Received(data) =>
      log.info(s"Received: $data ${data.length}")
      handleByteStream(BitVector(data.toArray))(handlePackage, handleError)

    case PeerClosed =>
      log.info("Connection closed by peer")
      context stop self

    case ErrorClosed =>
      log.error("ErrorClosed")
      context stop self

    case Closed =>
      log.info(s"Connection closed by listener")
      context stop self
  }
}
