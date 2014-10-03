package com.secretapp.backend.api.frontend

import akka.actor._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.persist.AuthIdRecord
import com.secretapp.backend.session.SessionProtocol
import com.secretapp.backend.session.SessionProtocol.{HandleMTMessageBox, HandleJsonMessageBox, Envelope}
import scala.collection.JavaConversions._
import scala.util.Success
import scodec.bits._
import scalaz._
import Scalaz._

object SecurityFrontend {
  def props(connection: ActorRef, sessionRegion: ActorRef, authId: Long, sessionId: Long, transport: TransportConnection)(implicit csession: CSession) = {
    Props(new SecurityFrontend(connection, sessionRegion, authId, sessionId, transport)(csession))
  }
}

class SecurityFrontend(connection: ActorRef, sessionRegion: ActorRef, authId: Long, sessionId: Long, transport: TransportConnection)(implicit csession: CSession) extends Actor
with ActorLogging
{
  import context.dispatcher
  import context.system

  val receiveBuffer = new java.util.LinkedList[Any]()
  val authRecF = AuthIdRecord.getEntityWithUser(authId)

  override def preStart(): Unit = {
    super.preStart()
    authRecF.onComplete {
      case Success(Some((authRec, userOpt))) =>
        userOpt.map { u => sessionRegion ! SessionProtocol.Envelope(authId, sessionId, SessionProtocol.AuthorizeUser(u)) } // TODO: guarantee delivery to session actor
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

  def receivePF: Receive = {
    case RequestPackage(p) =>
      log.info(s"RequestPackage: $p")
      if (p.sessionId == 0L) silentClose()
      else {
        if (p.sessionId == sessionId) {
          p.decodeMessageBox match {
            case \/-(mb) =>
              // TODO: validate client messageId
              sessionRegion.tell(Envelope(p.authId, p.sessionId, transport.wrapMessageBox(mb)), connection)
            case -\/(e) =>
              log.error(s"$e, p.messageBoxBytes: ${p.messageBoxBytes}, ${new String(p.messageBoxBytes.toByteArray)}")
              silentClose()
          }
        } else silentClose()
      }
    case SilentClose => silentClose()
  }

  def receive = {
    case m => receiveBuffer.add(m)
  }
}