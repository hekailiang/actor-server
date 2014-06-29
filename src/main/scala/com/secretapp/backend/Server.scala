package com.secretapp.backend

import akka.actor.{ Actor, ActorRef, ActorLogging, Props }
import akka.io.{ IO, Tcp }
import akka.util.ByteString
import Tcp._

class Server extends Actor with ActorLogging {

  import context.system

  def receive = {
    case b @ Bound(localAddress) =>
      log.info(s"Bound: $b")
    case CommandFailed(_: Bind) =>
      log.info("CommandFailed")
      context stop self
    case c @ Connected(remote, local) =>
      log.info(s"Connected: $c")
      val handler = context.actorOf(Props[ApiHandler])
      val connection = sender()
      connection ! Register(handler)
  }
}

class ApiHandler extends Actor with ActorLogging {
  def receive = {
    case Received(data) =>
      val connection = sender()
      val length = data.length
      log.info(s"Received: $data $length")
      connection ! Write(data)
      connection ! Close
    case PeerClosed     =>
      log.info("PeerClosed")
      context stop self
  }
}
