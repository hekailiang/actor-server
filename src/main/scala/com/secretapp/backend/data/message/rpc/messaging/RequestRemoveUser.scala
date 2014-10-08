package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

@SerialVersionUID(1l)
case class RequestRemoveUser(
  chatId: Int,
  accessHash: Long,
  userId: Int,
  userAccessHash: Long
) extends RpcRequestMessage {
  val header = RequestRemoveUser.requestType
}

object RequestRemoveUser extends RpcRequestMessageObject {
  val requestType = 0x47
}
