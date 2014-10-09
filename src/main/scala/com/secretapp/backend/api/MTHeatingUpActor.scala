package com.secretapp.backend.api

import java.net.InetSocketAddress
import akka.actor._
import akka.io.{ IO, Tcp }
import akka.util.ByteString
import scodec.bits._
import scala.concurrent.duration._

class MTHeatingUpActor(remote: InetSocketAddress) extends Actor {
  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)

  context.setReceiveTimeout(5.seconds)

  def receive = {
    case CommandFailed(_: Connect) =>
      context stop self
    case c @ Connected(_, local) =>
      val connection = sender()
      connection ! Register(self)
      connection ! Write(ByteString(hex"ffffffffffffffffffffffffff".bits.toByteArray))
      context become {
        case Received(_) =>
          connection ! Close
        case PeerClosed | ErrorClosed | Closed =>
          context stop self
      }
  }
}
