package com.secretapp.backend.data.message

case class RequestAuthId() extends TransportMessage
object RequestAuthId extends TransportMessageMessageObject {
  val header = 0xf0
}
