package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

case class RequestSendMessage(uid: Int,
                              accessHash: Long,
                              randomId: Long,
                              message: EncryptedMessage,
                              selfMessage: Option[EncryptedMessage]) extends RpcRequestMessage

object RequestSendMessage extends RpcRequestMessageObject {
  val requestType = 0x0E
}
