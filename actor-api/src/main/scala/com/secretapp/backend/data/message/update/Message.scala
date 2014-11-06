package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.struct.Peer
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class Message(peer: Peer,
                   senderUid: Int,
                   date: Long,
                   randomId: Long,
                   message: MessageContent) extends SeqUpdateMessage
{
  val header = Message.header

  def userIds: Set[Int] = Set(peer.id, senderUid)
}

object Message extends SeqUpdateMessageObject {
  val header = 0x1
}
