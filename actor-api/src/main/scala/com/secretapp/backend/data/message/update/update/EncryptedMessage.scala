package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.Peer
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class EncryptedMessage(peer: Peer, senderUid: Int, keyHash: Long, aesEncryptedKey: BitVector, message: BitVector, date: Long) extends SeqUpdateMessage {
  val header = EncryptedMessage.header

  def userIds = Set(peer.id)
}

object EncryptedMessage extends SeqUpdateMessageObject {
  val header = 0x01
}
