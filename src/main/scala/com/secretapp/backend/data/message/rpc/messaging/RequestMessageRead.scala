package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

case class RequestMessageRead(uid: Int, randomId: Long, accessHash: Long) extends RpcRequestMessage {
  override val header = RequestMessageRead.requestType
}

object RequestMessageRead extends RpcRequestMessageObject {
  override val requestType = 0x39
}
