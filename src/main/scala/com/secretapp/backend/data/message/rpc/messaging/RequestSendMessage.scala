package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

case class RequestSendMessage(uid: Int,
                              accessHash: Long,
                              randomId: Long,
                              useAesKey: Boolean,
                              aesMessage: Option[BitVector],
                              messages: immutable.Seq[EncryptedMessage]) extends RpcRequestMessage {
  override val header = RequestSendMessage.requestType
}
object RequestSendMessage extends RpcRequestMessageObject {
  override val requestType = 0xe
}
