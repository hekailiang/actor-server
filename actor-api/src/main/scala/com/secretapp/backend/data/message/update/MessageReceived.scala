package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class MessageReceived(peer: struct.Peer, date: Long, readDate: Long) extends SeqUpdateMessage {
  val header = MessageReceived.header

  def userIds: Set[Int] = Set(peer.id)
}

object MessageReceived extends SeqUpdateMessageObject {
  val header = 0x12
}
