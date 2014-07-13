package com.secretapp.backend.data.message

case class ResponseAuthId(authId : Long) extends TransportMessage
object ResponseAuthId extends TransportMessageMessageObject {
  val header = 0xf1
}
