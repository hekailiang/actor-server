package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

case class RequestSendGroupMessage(
  chatId: Int,
  accessHash: Long,
  randomId: Long,
  message: EncryptedAESMessage
) extends RpcRequestMessage

object RequestSendGroupMessage extends RpcRequestMessageObject {
  val requestType = 0x43
}
