package com.secretapp.backend.api

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.io.Tcp._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.counters.FilesCounter
import com.secretapp.backend.protocol.codecs.common.VarIntCodec
import com.secretapp.backend.services.rpc.presence.PresenceBroker
import scodec.bits.BitVector

final class Singletons(implicit val system: ActorSystem) {
  val filesCounter = FilesCounter.start(system)
  val presenceBroker = PresenceBroker.start(system)
}

final class ClusterProxies(implicit val system: ActorSystem) {
  val filesCounter = FilesCounter.startProxy(system)
  val presenceBroker = PresenceBroker.startProxy(system)
}

class Server(session: CSession) extends Actor with ActorLogging {
  import context.system

  val singletons = new Singletons
  val clusterProxies = new ClusterProxies

  def receive = {
    case b @ Bound(localAddress) =>
      log.info(s"Bound: $b")
    case CommandFailed(_: Bind) =>
      log.info("CommandFailed")
      context stop self
    case c @ Connected(remote, local) =>
      log.info(s"Connected: $c")
      val connection = sender()
      val handler = context.actorOf(Props(new ApiHandlerActor(connection, clusterProxies)(session)))
      connection ! Register(handler, keepOpenOnPeerClosed = true)
  }
}
