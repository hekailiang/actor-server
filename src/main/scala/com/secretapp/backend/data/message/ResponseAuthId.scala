package com.secretapp.backend.data.message

@SerialVersionUID(1L)
case class ResponseAuthId(authId: Long) extends TransportMessage {
  val header = ResponseAuthId.header
}

object ResponseAuthId extends TransportMessageMessageObject {
  val header = 0xF1
}
