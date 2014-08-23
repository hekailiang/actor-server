package com.secretapp.backend.api

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.io.Tcp._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.counters.FilesCounter
import com.secretapp.backend.protocol.codecs.common.VarIntCodec
import scodec.bits.BitVector

final class Counters(implicit val system: ActorSystem) {
  val files = FilesCounter.start(system)
}

final class CountersProxies(implicit val system: ActorSystem) {
  val files = FilesCounter.startProxy(system)
}

class Server(session: CSession) extends Actor with ActorLogging {
  import context.system

  val counters = new Counters
  val countersProxies = new CountersProxies

  def receive = {
    case b @ Bound(localAddress) =>
      log.info(s"Bound: $b")
    case CommandFailed(_: Bind) =>
      log.info("CommandFailed")
      context stop self
    case c @ Connected(remote, local) =>
      log.info(s"Connected: $c")
      val connection = sender()
      val handler = context.actorOf(Props(new ApiHandlerActor(connection, countersProxies)(session)))
      connection ! Register(handler)
  }
}
