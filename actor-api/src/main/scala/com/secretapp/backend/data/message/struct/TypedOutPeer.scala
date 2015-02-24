package com.secretapp.backend.data.message.struct

import com.secretapp.backend.models

private[struct] class TypedOutPeer(typ: models.PeerType, id: Int, accessHash: Long) {
  final lazy val asPeer = Peer(typ, id)
  final lazy val asOutPeer = OutPeer(typ, id, accessHash)
}
