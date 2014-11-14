package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestEditUserLocalName(userId: Int, accessHash: Long, name: String) extends RpcRequestMessage {
  val header = RequestEditUserLocalName.header
}

object RequestEditUserLocalName extends RpcRequestMessageObject {
  val header = 0x60
}
