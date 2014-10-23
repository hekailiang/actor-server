package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class RequestLeaveGroup(
  groupId: Int,
  accessHash: Long
) extends RpcRequestMessage {
  val header = RequestLeaveGroup.requestType
}

object RequestLeaveGroup extends RpcRequestMessageObject {
  val requestType = 0x46
}
