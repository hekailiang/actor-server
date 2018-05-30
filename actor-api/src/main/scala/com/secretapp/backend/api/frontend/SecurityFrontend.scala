package com.secretapp.backend.api.frontend

import akka.actor._
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.data.message.Drop
import com.secretapp.backend.persist
import com.secretapp.backend.session.SessionProtocol
import scala.collection.JavaConversions._
import scala.util.Success
import scalaz._
import Scalaz._

object SecurityFrontend {
  def props(frontend: ActorRef, sessionRegion: ActorRef, authId: Long, sessionId: Long) = {
    Props(new SecurityFrontend(frontend, sessionRegion, authId, sessionId))
  }
}

class SecurityFrontend(frontend: ActorRef, sessionRegion: ActorRef, authId: Long, sessionId: Long) extends Actor
with ActorLogging
{
  import context.system
  implicit val ec = context.dispatcher

  val receiveBuffer = new java.util.LinkedList[Any]()
  val authRecF = persist.AuthId.findWithUser(authId)

  override def preStart(): Unit = {
    super.preStart()
    authRecF.onComplete {
      case Success(Some((authRec, userOpt))) =>
        userOpt.map { u => sessionRegion ! SessionProtocol.Envelope(authId, sessionId, SessionProtocol.AuthorizeUser(u)) } // TODO: guarantee delivery to session actor
        context.become(receivePF)
        receiveBuffer.foreach(self ! _)
        receiveBuffer.clear()
      case e => silentClose(s"preStart: $e")
    }
  }

  def silentClose(reason: String): Unit = {
    log.error(s"$authId#SecurityFrontend.silentClose: $reason")
    // TODO
    val pkg = MTConnection.buildPackage(0L, 0, MessageBox(0, Drop(0, reason)))
    frontend ! ResponseToClientWithDrop(pkg.encode)
  }

  def receivePF: Receive = {
    case RequestPackage(p) =>
      //log.debug(s"$authId#RequestPackage: $p, name: ${self.path.name}")
      if (p.sessionId == 0L) silentClose("p.sessionId == 0L")
      else {
        if (p.sessionId == sessionId) {
          p.decodeMessageBox match {
            case \/-(mb) =>
//              TODO
//              if (mb.messageId % 4 == 0)
//                log.debug(s"$authId#Envelope: ${SessionProtocol.Envelope(p.authId, p.sessionId, transport.wrapMessageBox(mb))}, name: ${self.path.name}")
                sessionRegion.tell(SessionProtocol.Envelope(p.authId, p.sessionId, MTConnection.wrapMessageBox(mb)), frontend)
//              else silentClose()
            case -\/(e) =>
              log.error(s"$e, p.messageBoxBytes: ${p.messageBoxBytes}, ${p.messageBoxBytes.toHex}")
              silentClose(e)
          }
        } else silentClose("p.sessionId == sessionId")
      }
    case SilentClose => silentClose("SilentClose")
  }

  def receive = {
    case m => receiveBuffer.add(m)
  }
}
