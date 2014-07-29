package com.secretapp.backend.api

import java.net.InetSocketAddress
import akka.actor._
import akka.io.{ IO, Tcp }
import akka.kernel.Bootable
import akka.util.ByteString
import Tcp._
import scodec.bits._
import scala.concurrent.duration._

class HeatingUpActor(remote: InetSocketAddress) extends Actor {
  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)

  context.setReceiveTimeout(5.seconds)

  def receive = {
    case CommandFailed(_: Connect) =>
      context stop self
    case c @ Connected(remote, local) =>
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
