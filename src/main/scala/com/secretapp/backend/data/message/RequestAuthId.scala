package com.secretapp.backend.data.message

case class RequestAuthId() extends TransportMessage {
  override val header = RequestAuthId.header
}
object RequestAuthId extends TransportMessageMessageObject {
  override val header = 0xf0
}
