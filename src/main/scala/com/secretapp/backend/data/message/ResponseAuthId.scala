package com.secretapp.backend.data.message

case class ResponseAuthId(authId : Long) extends TransportMessage
object ResponseAuthId {
  val header = 0xf1
}
