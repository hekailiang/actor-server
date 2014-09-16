package com.secretapp.backend.data.message


case class RequestUnregisterPush() extends TransportMessage

object RequestUnregisterPush extends TransportMessageMessageObject {
  val header = 0x51
}
