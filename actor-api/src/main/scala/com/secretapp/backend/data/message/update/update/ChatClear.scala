package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.Peer

@SerialVersionUID(1L)
case class ChatClear(peer: Peer) extends SeqUpdateMessage {
  val header = ChatClear.header

  def userIds: Set[Int] = Set(peer.id)
}

object ChatClear extends SeqUpdateMessageObject {
  val header = 0x2F
}
