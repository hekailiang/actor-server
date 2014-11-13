package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.Peer

@SerialVersionUID(1L)
case class ChatDelete(peer: Peer) extends SeqUpdateMessage {
  val header = ChatDelete.header

  def userIds: Set[Int] = Set(peer.id)

  def groupIds: Set[Int] = Set.empty
}

object ChatDelete extends SeqUpdateMessageObject {
  val header = 0x30
}
