package com.secretapp.backend.data.transport

import akka.util.ByteString
import com.secretapp.backend.data.message.TransportMessage
import com.secretapp.backend.protocol.codecs.message.{JsonMessageBoxCodec, MessageBoxCodec}
import scodec.bits._

case class JsonPackage(authId: Long, sessionId: Long, messageBoxBytes: BitVector) extends TransportPackage {
  def decodeMessageBox = JsonMessageBoxCodec.decodeValue(this.messageBoxBytes)

  def encode = ByteString(s"[${this.authId},${this.sessionId},") ++ ByteString(this.messageBoxBytes.toByteBuffer) ++ ByteString("]")

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
