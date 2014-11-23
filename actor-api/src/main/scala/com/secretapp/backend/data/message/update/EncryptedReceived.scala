package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class EncryptedReceived(outPeer: struct.Peer, randomId: Long, receivedDate: Long) extends SeqUpdateMessage {
  val header = EncryptedReceived.header

  def userIds: Set[Int] = Set(outPeer.id)

  def groupIds: Set[Int] = Set.empty
}

object EncryptedReceived extends SeqUpdateMessageObject {
  val header = 0x12
}
