package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.Peer

import scala.collection.immutable

@SerialVersionUID(1L)
case class MessageDelete(peer: Peer, randomIds: immutable.Seq[Long]) extends SeqUpdateMessage {
  val header = MessageDelete.header

  def userIds: Set[Int] = Set(peer.id)
}

object MessageDelete extends SeqUpdateMessageObject {
  val header = 0x2E
}
