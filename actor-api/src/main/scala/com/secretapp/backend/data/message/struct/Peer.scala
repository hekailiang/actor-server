package com.secretapp.backend.data.message.struct

import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class Peer(kind: PeerType, id: Int) {
  def toProto = protobuf.Peer(kind.toProto, id)
}

object Peer {
  def fromProto(p: protobuf.Peer) = Peer(PeerType.fromProto(p.`type`), p.id)
}
