package com.secretapp.backend.session

import com.secretapp.backend.api.frontend._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.protocol.codecs.message._
import scodec.bits.BitVector

trait TransportSerializers {
  val authId: Long
  val sessionId: Long

//  val transport: Option[TransportConnection] = Some(MTConnection)

  def serializeMessageBox(message: MessageBox): BitVector =
    MessageBoxCodec.encodeValid(message)

  def serializePackage(mb: BitVector): ResponseToClient =
    ResponseToClient(MTPackage(authId, sessionId, mb).encode)

  def serializePackage(message: MessageBox): ResponseToClient =
    serializePackage(serializeMessageBox(message))
}
