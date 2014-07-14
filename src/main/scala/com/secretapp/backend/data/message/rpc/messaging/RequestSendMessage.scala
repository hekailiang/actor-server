package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable.Seq
import com.secretapp.backend.data.message.rpc._

case class RequestSendMessage(uid : Int,
                              accessHash : Long,
                              useAesKey : Boolean,
                              aesMessage : Option[List[Byte]],
                              messages : Seq[EncryptedMessage]) extends RpcRequestMessage
object RequestSendMessage extends RpcRequestMessageObject {
  val requestType = 0xe
}
