package com.secretapp.backend.data.message

case class RequestRegisterGooglePush() extends TransportMessage

object RequestRegisterGooglePush extends TransportMessageMessageObject {
  val header = 0x50
}
