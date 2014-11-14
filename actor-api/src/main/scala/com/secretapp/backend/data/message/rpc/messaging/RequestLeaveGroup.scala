package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class RequestLeaveGroup(groupOutPeer: struct.GroupOutPeer) extends RpcRequestMessage {
  val header = RequestLeaveGroup.header
}

object RequestLeaveGroup extends RpcRequestMessageObject {
  val header = 0x46
}
