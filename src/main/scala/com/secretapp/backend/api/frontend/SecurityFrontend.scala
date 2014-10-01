package com.secretapp.backend.api.frontend

import akka.actor._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.persist.AuthIdRecord
import com.secretapp.backend.session.SessionProtocol.{HandleMTMessageBox, HandleJsonMessageBox, HandleMessageBox, Envelope}
import scala.collection.JavaConversions._
import scala.util.Success
import scodec.bits._
import scalaz._
import Scalaz._

object SecurityFrontend {
  def props(connection: ActorRef, sessionRegion: ActorRef, authId: Long, transport: TransportConnection)(implicit csession: CSession) = {
    Props(new SecurityFrontend(connection, sessionRegion, authId, transport)(csession))
  }
}

class SecurityFrontend(connection: ActorRef, sessionRegion: ActorRef, authId: Long, transport: TransportConnection)(implicit csession: CSession) extends Actor
with ActorLogging
{
  import context.dispatcher
  import context.system

  val receiveBuffer = new java.util.LinkedList[Any]()
  val authRecF = AuthIdRecord.getEntity(authId)
  var sessionId: Long = _ // last sessionId from EncryptPackage's

  override def preStart(): Unit = {
    super.preStart()
    authRecF.onComplete {
      case Success(Some(authRec)) =>
        context.become(receivePF)
        receiveBuffer.foreach(self ! _)
        receiveBuffer.clear()
      case _ => silentClose()
    }
  }

  def silentClose(): Unit = {
    connection ! SilentClose
    context stop self
  }

  @inline
  private def wrapMessageBox(mb: MessageBox) = transport match {
    case JsonConnection => HandleJsonMessageBox(mb)
    case MTConnection => HandleMTMessageBox(mb)
  }

  def receivePF: Receive = {
    case RequestPackage(p) =>
      log.info(s"RequestPackage: $p")

      if (p.sessionId == 0L) silentClose()
      else {
        if (sessionId == 0L) sessionId = p.sessionId
        if (p.sessionId == sessionId) {
          p.decodeMessageBox match {
            case \/-(mb) =>
              println(s"sessionRegion.tell: ${wrapMessageBox(mb)}")
              sessionRegion.tell(Envelope(p.authId, p.sessionId, wrapMessageBox(mb)), connection)
            case -\/(e) => ???
          }
        } else silentClose()
      }
    case SilentClose => silentClose()
    case x =>
      println(s"sessionactor: $x")
  }

  def receive = {
    case m: Any => receiveBuffer.add(m)
  }
}
