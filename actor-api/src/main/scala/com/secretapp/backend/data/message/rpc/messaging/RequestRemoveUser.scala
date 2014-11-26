package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.struct

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestRemoveUser(groupOutPeer: struct.GroupOutPeer, user: struct.UserOutPeer) extends RpcRequestMessage {
  val header = RequestRemoveUser.header
}

object RequestRemoveUser extends RpcRequestMessageObject {
  val header = 0x47
}
