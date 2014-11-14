package com.secretapp.backend.data.message.struct

import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class OutPeer(typ: PeerType, id: Int, accessHash: Long) {
  def toProto = protobuf.OutPeer(typ.toProto, id, accessHash)

  lazy val asPeer = Peer(typ, id)
}

object OutPeer {
  def fromProto(p: protobuf.OutPeer) = OutPeer(PeerType.fromProto(p.`type`), p.id, p.accessHash)

  def privat(userId: Int, accessHash: Long) = OutPeer(PeerType.Private, userId, accessHash)

  def group(groupId: Int, accessHash: Long) = OutPeer(PeerType.Group, groupId, accessHash)
}
