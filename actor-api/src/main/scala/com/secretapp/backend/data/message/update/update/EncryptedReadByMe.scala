package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class EncryptedReadByMe(peer: struct.Peer, randomId: Long) extends SeqUpdateMessage {
  val header = EncryptedReadByMe.header

  def userIds: Set[Int] = Set(peer.id)
}

object EncryptedReadByMe extends SeqUpdateMessageObject {
  val header = 0x35
}
