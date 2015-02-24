package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.models
import im.actor.messenger.{ api => protobuf }
import shapeless._
import Function.tupled

@SerialVersionUID(1L)
case class GroupOutPeer(id: Int, accessHash: Long)
    extends TypedOutPeer(models.PeerType.Group, id, accessHash)
    with ProtobufMessage {
  def toProto = (protobuf.GroupOutPeer.apply _).tupled(GroupOutPeer.unapply(this).get)
}

object GroupOutPeer {
  def fromProto(p: protobuf.GroupOutPeer) = (GroupOutPeer.apply _).tupled(protobuf.GroupOutPeer.unapply(p).get)
}
