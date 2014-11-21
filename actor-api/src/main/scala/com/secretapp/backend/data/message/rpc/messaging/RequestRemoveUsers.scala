package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.struct

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestRemoveUsers(groupOutPeer: struct.GroupOutPeer, users: immutable.Seq[struct.UserOutPeer]) extends RpcRequestMessage {
  val header = RequestRemoveUsers.header
}

object RequestRemoveUsers extends RpcRequestMessageObject {
  val header = 0x47
}
