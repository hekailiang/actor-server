package com.secretapp.backend.data.message

import com.secretapp.backend.data.message.update.UpdateMessage

@SerialVersionUID(1L)
case class UpdateBox(body: UpdateMessage) extends TransportMessage {
  val header = UpdateBox.header
}

object UpdateBox extends TransportMessageMessageObject {
  val header = 0x05
}
