package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable.Seq
import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

case class RequestSendMessage(uid : Int,
                              accessHash : Long,
                              useAesKey : Boolean,
                              aesMessage : Option[BitVector],
                              messages : Seq[EncryptedMessage]) extends RpcRequestMessage
object RequestSendMessage extends RpcRequestMessageObject {
  val requestType = 0xe
}
