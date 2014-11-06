package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.struct
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class OutPeer(peerType: struct.PeerType, id: Int, accessHash: Long) {
  def toProto = protobuf.OutPeer(peerType.toProto, id, accessHash)
}

object OutPeer {
  def fromProto(p: protobuf.OutPeer) = OutPeer(struct.PeerType.fromProto(p.`type`), p.id, p.accessHash)
}
