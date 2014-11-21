package com.secretapp.backend.data.message.rpc

import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class RequestDeleteGroup(groupPeer: struct.GroupOutPeer) extends RpcRequestMessage {
  val header = RequestDeleteGroup.header
}

object RequestDeleteGroup extends RpcRequestMessageObject {
  val header = 0x61
}
