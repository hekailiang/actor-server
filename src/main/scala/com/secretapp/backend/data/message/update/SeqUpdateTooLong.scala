package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.rpc._

case class SeqUpdateTooLong() extends UpdateMessage {
  val updateHeader = 0x19
}

object SeqUpdateTooLong extends UpdateMessageObject {
  val updateHeader = 0x19
}
