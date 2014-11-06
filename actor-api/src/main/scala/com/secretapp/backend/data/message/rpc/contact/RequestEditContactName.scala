package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestEditContactName(uid: Int, accessHash: Long, name: String) extends RpcRequestMessage {
  val header = RequestEditContactName.header
}

object RequestEditContactName extends RpcRequestMessageObject {
  val header = 0x60
}
