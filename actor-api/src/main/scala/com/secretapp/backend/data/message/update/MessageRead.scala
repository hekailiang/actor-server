package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.Peer

@SerialVersionUID(1L)
case class MessageRead(peer: Peer, randomId: Long, readDate: Long) extends SeqUpdateMessage {
  val header = MessageRead.header

  def userIds: Set[Int] = Set(peer.id)
}

object MessageRead extends SeqUpdateMessageObject {
  val header = 0x13
}
