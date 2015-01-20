package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.models
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class Message(peer: struct.Peer,
                   senderUid: Int,
                   date: Long,
                   randomId: Long,
                   message: MessageContent) extends SeqUpdateMessage
{
  val header = Message.header

  def userIds: Set[Int] = Set(peer.id, senderUid)

  def groupIds: Set[Int] = peer.typ match {
    case models.PeerType.Group => Set(peer.id)
    case _ => Set.empty
  }
}

object Message extends SeqUpdateMessageObject {
  val header = 0x37
}
