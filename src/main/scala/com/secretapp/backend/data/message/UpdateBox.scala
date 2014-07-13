package com.secretapp.backend.data.message

import com.secretapp.backend.data.message.update.UpdateMessage

case class UpdateBox(body : UpdateMessage) extends TransportMessage
object UpdateBox {
  val header = 0x5
}
