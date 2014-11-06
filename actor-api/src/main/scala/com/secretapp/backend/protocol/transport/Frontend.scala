package com.secretapp.backend.protocol.transport

import akka.actor._
import akka.util.Timeout
import com.secretapp.backend.api.frontend.KeyFrontend.InitDH
import com.secretapp.backend.api.frontend._
import com.secretapp.backend.data.message.Drop
import com.secretapp.backend.data.transport._
import com.datastax.driver.core.{ Session => CSession }
import scalaz._
import Scalaz._
import scala.collection.mutable
import java.net.InetSocketAddress

trait Frontend extends Actor with ActorLogging {
  import scala.concurrent.duration._

  val connection: ActorRef
  val sessionRegion: ActorRef
  val transport: TransportConnection
  val session: CSession
  val remote: InetSocketAddress

  val keyFrontend: Option[ActorRef] = None
  val secFrontend = mutable.HashMap[Long, ActorRef]()

  var authId = 0L

  implicit val timeout: Timeout = Timeout(5.seconds)

  context.watch(connection)

  def handlePackage(p: TransportPackage): Unit = {
    if (p.authId == 0L) {
      val keyFrontRef = keyFrontend match {
        case Some(keyRef) => keyRef
        case None =>
          val keyRef = context.system.actorOf(KeyFrontend.props(self, transport)(session))
          context.watch(keyRef)
          keyRef
      }
      keyFrontRef ! InitDH(p)
    } else {
      if (authId == 0L) authId = p.authId
      if (p.authId != authId) silentClose("p.authId != authId")
      else {
        val secFrontRef = secFrontend.getOrElse(p.sessionId, {
          val secRef = context.system.actorOf(SecurityFrontend.props(self, sessionRegion, p.authId, p.sessionId, transport)(session))
          secFrontend += Tuple2(p.sessionId, secRef)
          context.watch(secRef)
          secRef
        })
        log.debug(s"$authId#secFrontRef ! RequestPackage($p)")
        secFrontRef ! RequestPackage(p)
      }
    }
  }

  def sendDrop(msg: String): Unit = {
    log.error(msg)
    // TODO: silentClose() ???
    val reply = transport.buildPackage(0L, 0L, MessageBox(0L, Drop(0L, msg)))
    self ! ResponseToClientWithDrop(reply.encode)
  }

  def silentClose(reason: String): Unit
}
