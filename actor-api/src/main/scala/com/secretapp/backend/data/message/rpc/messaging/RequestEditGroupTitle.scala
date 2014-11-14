package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc.{ RpcRequestMessageObject, RpcRequestMessage }
import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class RequestEditGroupTitle(groupOutPeer: struct.GroupOutPeer, title: String) extends RpcRequestMessage {
  val header = RequestEditGroupTitle.header
}

object RequestEditGroupTitle extends RpcRequestMessageObject {
  val header = 0x55
}
