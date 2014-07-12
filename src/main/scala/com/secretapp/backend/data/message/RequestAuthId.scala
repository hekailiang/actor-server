package com.secretapp.backend.data.message

case class RequestAuthId() extends TransportMessage
object RequestAuthId {
  val header = 0xf0
}
