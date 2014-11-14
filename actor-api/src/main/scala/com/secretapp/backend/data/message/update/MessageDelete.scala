package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct

import scala.collection.immutable

@SerialVersionUID(1L)
case class MessageDelete(peer: struct.Peer, randomIds: immutable.Seq[Long]) extends SeqUpdateMessage {
  val header = MessageDelete.header

  def userIds: Set[Int] = Set(peer.id)

  def groupIds: Set[Int] = peer.kind match {
    case struct.PeerType.Group =>
      Set(peer.id)
    case _ =>
      Set.empty
  }
}

object MessageDelete extends SeqUpdateMessageObject {
  val header = 0x2E
}
