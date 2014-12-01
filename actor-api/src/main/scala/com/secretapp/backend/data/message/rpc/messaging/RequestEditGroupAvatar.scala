package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.struct
import com.secretapp.backend.models
import com.secretapp.backend.data.message.rpc.{ RpcRequestMessageObject, RpcRequestMessage }

@SerialVersionUID(1L)
case class RequestEditGroupAvatar(groupPeer: struct.GroupOutPeer, randomId: Long, fileLocation: models.FileLocation) extends RpcRequestMessage {
  val header = RequestEditGroupAvatar.header
}

object RequestEditGroupAvatar extends RpcRequestMessageObject {
  val header = 0x56
}
