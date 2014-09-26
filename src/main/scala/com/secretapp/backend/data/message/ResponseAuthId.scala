package com.secretapp.backend.data.message

case class ResponseAuthId(authId: Long) extends TransportMessage {
  override val header = ResponseAuthId.header
}
object ResponseAuthId extends TransportMessageMessageObject {
  override val header = 0xf1
}
