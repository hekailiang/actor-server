package com.secretapp.backend.data.message.struct

import com.secretapp.backend.models
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class OutPeer(typ: models.PeerType, id: Int, accessHash: Long) {
  def toProto = protobuf.OutPeer(
    Peer.typeToProto(typ),
    id,
    accessHash
  )

  lazy val asPeer = Peer(typ, id)
}

object OutPeer {
  def fromProto(p: protobuf.OutPeer) = OutPeer(
    models.PeerType.fromInt(p.`type`.id),
    p.id,
    p.accessHash
  )

  def privat(userId: Int, accessHash: Long) = OutPeer(models.PeerType.Private, userId, accessHash)

  def group(groupId: Int, accessHash: Long) = OutPeer(models.PeerType.Group, groupId, accessHash)
}
