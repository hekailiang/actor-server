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

trait Frontend extends Actor with ActorLogging {
  import scala.concurrent.duration._

  val sessionRegion: ActorRef
  val transport: TransportConnection
  val session: CSession

  lazy val keyFrontend: ActorRef = context.system.actorOf(KeyFrontend.props(self, transport)(session))
  val secFrontend = mutable.HashMap[Long, ActorRef]()

  var authId = 0L

  implicit val timeout: Timeout = Timeout(5.seconds)

  def handlePackage(p: TransportPackage): Unit = {
    if (p.authId == 0L) keyFrontend ! InitDH(p)
    else {
      if (authId == 0L) authId = p.authId
      if (p.authId != authId) silentClose()
      else secFrontend.get(p.sessionId) match {
        case Some(secRef) => secRef ! RequestPackage(p)
        case None =>
          val secRef = context.system.actorOf(SecurityFrontend.props(self, sessionRegion, p.authId, p.sessionId, transport)(session))
          secFrontend += Tuple2(p.sessionId, secRef)
          secRef ! RequestPackage(p)
      }
    }
  }

  def sendDrop(msg: String): Unit = {
    log.error(msg)
    // TODO: silentClose() ???
    val reply = transport.buildPackage(0L, 0L, MessageBox(0L, Drop(0L, msg)))
    self ! ResponseToClientWithDrop(reply.encode)
  }

  def silentClose(): Unit
}
