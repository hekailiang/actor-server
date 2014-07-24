package com.secretapp.backend.data.message

import com.secretapp.backend.data.message.update.CommonUpdateMessage

case class UpdateBox(body: CommonUpdateMessage) extends TransportMessage
object UpdateBox extends TransportMessageMessageObject {
  val header = 0x5
}
