package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class RequestEncryptedRead(outPeer: struct.OutPeer, randomId: Long) extends RpcRequestMessage {
  val header = RequestEncryptedRead.header
}

object RequestEncryptedRead extends RpcRequestMessageObject {
  val header = 0x75
}
