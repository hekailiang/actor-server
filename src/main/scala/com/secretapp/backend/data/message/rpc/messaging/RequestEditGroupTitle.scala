package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc.{ RpcRequestMessageObject, RpcRequestMessage }

@SerialVersionUID(1L)
case class RequestEditGroupTitle(groupId: Int, accessHash: Long, title: String) extends RpcRequestMessage {
  val header = RequestEditGroupTitle.requestType
}

object RequestEditGroupTitle extends RpcRequestMessageObject {
  val requestType = 0x55
}
