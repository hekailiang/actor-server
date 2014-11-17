package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class RequestEncryptedReceived(peer: struct.OutPeer, randomId: Long) extends RpcRequestMessage {
  val header = RequestEncryptedReceived.header
}

object RequestEncryptedReceived extends RpcRequestMessageObject {
  val header = 0x74
}
