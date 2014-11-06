package com.secretapp.backend.api

import akka.util.ByteString
import com.secretapp.backend.data.transport.{MTPackage, MessageBox, TransportPackage}
import com.secretapp.backend.session.SessionProtocol.{SessionMessage, HandleMTMessageBox, HandleMessageBox}

package object frontend {
  sealed trait ClientFrontendMessage

  @SerialVersionUID(1L)
  case object SilentClose extends ClientFrontendMessage // with ControlMessage

  @SerialVersionUID(1L)
  case class ResponseToClient(payload: ByteString) extends ClientFrontendMessage

  @SerialVersionUID(1L)
  case class ResponseToClientWithDrop(payload: ByteString) extends ClientFrontendMessage


  case class RequestPackage(p: TransportPackage)


  sealed trait TransportConnection {
    def buildPackage(authId: Long, sessionId: Long, message: MessageBox): TransportPackage

    def wrapMessageBox(mb: MessageBox): HandleMessageBox with SessionMessage
  }

  @SerialVersionUID(1L)
  case object MTConnection extends TransportConnection {
    def buildPackage(authId: Long, sessionId: Long, message: MessageBox) = MTPackage.build(authId, sessionId, message)

    @inline
    def wrapMessageBox(mb: MessageBox) = HandleMTMessageBox(mb)
  }
}
