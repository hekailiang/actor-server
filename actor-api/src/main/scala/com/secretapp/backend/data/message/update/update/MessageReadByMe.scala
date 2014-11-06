package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.Peer

@SerialVersionUID(1L)
case class MessageReadByMe(peer: Peer, readDate: Long) extends SeqUpdateMessage {
  val header = MessageReadByMe.header

  def userIds: Set[Int] = Set(peer.id)
}

object MessageReadByMe extends SeqUpdateMessageObject {
  val header = 0x32
}
