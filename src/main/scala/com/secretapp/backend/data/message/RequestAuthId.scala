package com.secretapp.backend.data.message

@SerialVersionUID(1L)
case class RequestAuthId() extends TransportMessage {
  val header = RequestAuthId.header
}

object RequestAuthId extends TransportMessageMessageObject {
  val header = 0xF0
}
