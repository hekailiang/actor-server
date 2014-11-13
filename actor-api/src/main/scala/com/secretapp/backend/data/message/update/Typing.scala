package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct

case class Typing(peer: struct.Peer, userId: Int, typingType: Int) extends WeakUpdateMessage {
  val header = Typing.header
}

object Typing extends WeakUpdateMessageObject {
  val header = 0x06
}
