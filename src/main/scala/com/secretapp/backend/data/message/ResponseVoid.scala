package com.secretapp.backend.data.message

case class ResponseVoid() extends TransportMessage

object ResponseVoid extends TransportMessageMessageObject {
  override val header = 0x32
}
