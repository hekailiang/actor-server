package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class MessageReceived(peer: struct.Peer, startDate: Long, receivedDate: Long) extends SeqUpdateMessage {
  val header = MessageReceived.header

  def userIds: Set[Int] = Set(peer.id)

  def groupIds: Set[Int] = peer.typ match {
    case struct.PeerType.Group =>
      Set(peer.id)
    case _ =>
      Set.empty
  }
}

object MessageReceived extends SeqUpdateMessageObject {
  val header = 0x36
}
