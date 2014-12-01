package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.struct

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestKickUser(groupOutPeer: struct.GroupOutPeer, randomId: Long, user: struct.UserOutPeer) extends RpcRequestMessage {
  val header = RequestKickUser.header
}

object RequestKickUser extends RpcRequestMessageObject {
  val header = 0x47
}
