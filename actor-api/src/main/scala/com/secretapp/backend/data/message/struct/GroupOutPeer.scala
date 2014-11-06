package com.secretapp.backend.data.message.struct

import im.actor.messenger.{ api => protobuf }
import shapeless._
import Function.tupled

@SerialVersionUID(1L)
case class GroupOutPeer(groupId: Int, accessHash: Long) {
  def toProto = (protobuf.GroupOutPeer.apply _).tupled(GroupOutPeer.unapply(this).get)
}

object GroupOutPeer {
  def fromProto(p: protobuf.GroupOutPeer) = (GroupOutPeer.apply _).tupled(protobuf.GroupOutPeer.unapply(p).get)
}
