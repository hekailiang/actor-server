package com.secretapp.backend.api.frontend

import akka.actor._
import com.secretapp.backend.data.message.{ RequestAuthId, ResponseAuthId, Drop }
import com.secretapp.backend.models
import com.secretapp.backend.persist
import com.secretapp.backend.data.transport._
import com.secretapp.backend.services.common.RandomService
import com.datastax.driver.core.{ Session => CSession }
import scalaz._
import Scalaz._

object KeyFrontend {
  sealed trait KeyInitializationMessage
  @SerialVersionUID(1L)
  case class InitDH(p: TransportPackage) extends KeyInitializationMessage

  def props(connection: ActorRef, transport: TransportConnection)(implicit csession: CSession): Props = {
    Props(new KeyFrontend(connection, transport)(csession))
  }
}

class KeyFrontend(connection: ActorRef, transport: TransportConnection)(implicit csession: CSession) extends Actor with ActorLogging with RandomService {
  import KeyFrontend._

  def silentClose(reason: String): Unit = {
    log.error(s"KeyFrontend.silentClose: $reason")
    // TODO
    val pkg = transport.buildPackage(0L, 0, MessageBox(0, Drop(0, reason)))
    connection ! ResponseToClientWithDrop(pkg.encode)
    connection ! SilentClose
//    context stop self
  }

  def receive = {
    case InitDH(p) =>
      p.decodeMessageBox match {
        case \/-(message) =>
          message.body match {
            case _ if p.sessionId != 0L =>
              dropClient(message.messageId, "sessionId must equal to 0", p.sessionId)
            case RequestAuthId() =>
              val newAuthId = rand.nextLong()
              persist.AuthId.insertEntity(models.AuthId(newAuthId, None))
              val pkg = transport.buildPackage(0L, 0L, MessageBox(message.messageId, ResponseAuthId(newAuthId)))
              connection ! ResponseToClient(pkg.encode)
            case _ =>
              dropClient(message.messageId, "unknown message type in authorize mode")
          }
        case -\/(e) =>
          log.error(e)
          silentClose(e)
      }
  }

  def dropClient(messageId: Long, message: String, sessionId: Long = 0): Unit = {
    val pkg = transport.buildPackage(0L, sessionId, MessageBox(messageId, Drop(messageId, message)))
    connection ! ResponseToClientWithDrop(pkg.encode)
  }
}
