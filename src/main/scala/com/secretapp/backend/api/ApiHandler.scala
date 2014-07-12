package com.secretapp.backend.api

import akka.actor.{ Actor, ActorRef, ActorLogging }
import akka.util.ByteString
import akka.io.Tcp._
import scodec.bits._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data._
import scalaz._
import Scalaz._
import com.datastax.driver.core.{ Session => CSession }


// TODO: replace connection : ActorRef hack with real sender (or forget it?)
class ApiHandler(connection : ActorRef, val session : CSession) extends Actor with ActorLogging
with WrappedPackageService with PackageHandler
{
  val handleActor = self

  def receive = {
    case PackageToSend(pe) => log.info(s"PackageToSend($pe)"); pe match {
      case \/-(p) =>
        connection ! Write(replyPackage(p))
      case -\/(p) =>
        connection ! Write(replyPackage(p))
        connection ! Close
        context stop self
    }

    case Received(data) =>
      log.info(s"Received: $data ${data.length}")
      handleByteStream(BitVector(data.toArray))(handlePackage, handleError)

    case PeerClosed =>
      log.info("PeerClosed")
      context stop self
  }

}
