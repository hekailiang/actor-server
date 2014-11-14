package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.rpc._
import scala.collection.immutable

@SerialVersionUID(1L)
case class RequestInviteUsers(groupOutPeer: struct.GroupOutPeer, users: immutable.Seq[struct.UserOutPeer]) extends RpcRequestMessage {
  val header = RequestInviteUsers.header
}

object RequestInviteUsers extends RpcRequestMessageObject {
  val header = 0x45
}
