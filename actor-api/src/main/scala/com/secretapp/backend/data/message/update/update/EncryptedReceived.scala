package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class EncryptedReceived(peer: struct.Peer, randomId: Long) extends SeqUpdateMessage {
  val header = EncryptedReceived.header

  def userIds: Set[Int] = Set(peer.id)
}

object EncryptedReceived extends SeqUpdateMessageObject {
  val header = 0x12
}
