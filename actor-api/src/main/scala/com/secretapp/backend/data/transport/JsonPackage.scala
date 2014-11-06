package com.secretapp.backend.data.transport

import com.secretapp.backend.protocol.codecs.message.JsonMessageBoxCodec
import com.secretapp.backend.protocol.transport.JsonPackageCodec
import scodec.bits._

@SerialVersionUID(1L)
case class JsonPackage(authId: Long, sessionId: Long, messageBoxBytes: BitVector) extends TransportPackage {
  def decodeMessageBox = JsonMessageBoxCodec.decode(this.messageBoxBytes)

  def encode = JsonPackageCodec.encodeValid(this)

  def build(authId: Long, sessionId: Long, message: MessageBox) = JsonPackage.build(authId, sessionId, message)
}

object JsonPackage {
  import play.api.libs.json.Json
  import com.secretapp.backend.data.json.message._

  def build(authId: Long, sessionId: Long, message: MessageBox) = {
    val json = Json.stringify(Json.toJson(message))
    JsonPackage(authId, sessionId, BitVector(json.getBytes))
  }
}
