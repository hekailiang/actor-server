package com.secretapp.backend.data.message

import com.secretapp.backend.data.message.update.UpdateMessage

case class UpdateBox(body: UpdateMessage) extends TransportMessage {
  override val header = UpdateBox.header
}
object UpdateBox extends TransportMessageMessageObject {
  override val header = 0x5
}
