package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class RequestRemoveUser(
  groupId: Int,
  accessHash: Long,
  userId: Int,
  userAccessHash: Long
) extends RpcRequestMessage {
  val header = RequestRemoveUser.header
}

object RequestRemoveUser extends RpcRequestMessageObject {
  val header = 0x47
}
