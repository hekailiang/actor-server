package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class RequestRemoveGroupAvatar(outPeer: struct.GroupOutPeer) extends RpcRequestMessage {
  val header = RequestRemoveGroupAvatar.header
}

object RequestRemoveGroupAvatar extends RpcRequestMessageObject {
  val header = 0x65
}
