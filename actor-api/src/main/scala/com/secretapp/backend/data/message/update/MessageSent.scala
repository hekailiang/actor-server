package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.Peer

@SerialVersionUID(1L)
case class MessageSent(peer: Peer, randomId: Long, date: Long) extends SeqUpdateMessage {
  val header = MessageSent.header

  def userIds: Set[Int] = Set(peer.id)
}

object MessageSent extends SeqUpdateMessageObject {
  val header = 0x04
}
