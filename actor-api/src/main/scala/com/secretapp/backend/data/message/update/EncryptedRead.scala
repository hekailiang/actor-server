package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class EncryptedRead(peer: struct.Peer, randomId: Long) extends SeqUpdateMessage {
  val header = EncryptedRead.header

  def userIds: Set[Int] = Set(peer.id)

  def groupIds: Set[Int] = Set.empty
}

object EncryptedRead extends SeqUpdateMessageObject {
  val header = 0x34
}
