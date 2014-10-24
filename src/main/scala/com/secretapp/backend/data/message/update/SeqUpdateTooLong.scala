package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class SeqUpdateTooLong() extends UpdateMessage {
  val header = 0x19
}

object SeqUpdateTooLong extends UpdateMessageObject {
  val header = 0x19
}
