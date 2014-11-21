package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class UserOutPeer(id: Int, accessHash: Long) extends ProtobufMessage {
  def toProto = (protobuf.UserOutPeer.apply _).tupled(UserOutPeer.unapply(this).get)
}

object UserOutPeer {
  def fromProto(p: protobuf.UserOutPeer) = (UserOutPeer.apply _).tupled(protobuf.UserOutPeer.unapply(p).get)
}
