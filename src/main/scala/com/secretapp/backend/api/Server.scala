package com.secretapp.backend.api

import akka.actor._
import akka.io.Tcp._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.counters.FilesCounter
import com.secretapp.backend.protocol.codecs.common.VarIntCodec
import com.secretapp.backend.services.rpc.presence.PresenceBroker
import com.secretapp.backend.session._
import com.secretapp.backend.protocol.transport._
import scodec.bits.BitVector

object Server {
  def props(sessionRegion: ActorRef)(implicit session: CSession) = Props(new Server(sessionRegion))
}

class Server(sessionRegion: ActorRef)(implicit session: CSession) extends Actor with ActorLogging {
  import context.system

  def receive = {
    case b @ Bound(localAddress) =>
      log.info(s"Bound: $b")
    case CommandFailed(_: Bind) =>
      log.info("CommandFailed")
      context stop self
    case c @ Connected(remote, local) =>
      log.info(s"Connected: $c")
      val connection = sender()
      val frontendActor = context.actorOf(Props(new TcpConnector(connection, sessionRegion, session)))
      connection ! Register(frontendActor, keepOpenOnPeerClosed = true)
  }
}
