package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.rpc._

case class CommonUpdateTooLong() extends CommonUpdateMessage {
  val commonUpdateType = 0x19
}

object CommonUpdateTooLong extends UpdateMessageObject {
  val updateType = 0x19
}
