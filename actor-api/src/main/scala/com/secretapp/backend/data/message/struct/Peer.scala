package com.secretapp.backend.data.message.struct

import com.secretapp.backend.models
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class Peer(typ: models.PeerType, id: Int) {
  def toProto = protobuf.Peer(Peer.typeToProto(typ), id)

  lazy val asModel = models.Peer(typ, id)
}

object Peer {
  def typeToProto(typ: models.PeerType) = protobuf.PeerType.valueOf(typ.toInt)

  def fromModel(peerModel: models.Peer): Peer = Peer(
    peerModel.typ, peerModel.id
  )

  def fromProto(p: protobuf.Peer) = Peer(models.PeerType.fromInt(p.`type`.id), p.id)

  def privat(userId: Int) = Peer(models.PeerType.Private, userId)

  def group(groupId: Int) = Peer(models.PeerType.Group, groupId)
}
