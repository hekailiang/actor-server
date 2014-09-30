package com.secretapp.backend.api

import akka.util.ByteString
import com.secretapp.backend.data.transport.{MTPackage, JsonPackage, MessageBox, TransportPackage}

package object frontend {
  sealed trait ClientFrontendMessage

  @SerialVersionUID(1L)
  case object SilentClose extends ClientFrontendMessage // with ControlMessage

  @SerialVersionUID(1L)
  case class ResponseToClient(payload: ByteString) extends ClientFrontendMessage

  @SerialVersionUID(1L)
  case class ResponseToClientWithDrop(payload: ByteString) extends ClientFrontendMessage


  sealed trait TransportConnection {
    def buildPackage(authId: Long, sessionId: Long, message: MessageBox): TransportPackage
  }

  @SerialVersionUID(1L)
  case object JsonConnection extends TransportConnection {
    def buildPackage(authId: Long, sessionId: Long, message: MessageBox) = JsonPackage.build(authId, sessionId, message)
  }

  @SerialVersionUID(1L)
  case object BinaryConnection extends TransportConnection {
    def buildPackage(authId: Long, sessionId: Long, message: MessageBox) = MTPackage.build(authId, sessionId, message)
  }
}
