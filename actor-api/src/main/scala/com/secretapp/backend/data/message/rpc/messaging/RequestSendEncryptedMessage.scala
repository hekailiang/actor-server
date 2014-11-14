package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct
import scodec.bits.BitVector

import scala.collection.immutable

@SerialVersionUID(1L)
case class RequestSendEncryptedMessage(outPeer: struct.OutPeer,
                                       randomId: Long,
                                       encryptedMessage: BitVector,
                                       keys: immutable.Seq[EncryptedAESKey],
                                       ownKeys: immutable.Seq[EncryptedAESKey]) extends RequestWithRandomId {
  val header = RequestSendEncryptedMessage.header
}

object RequestSendEncryptedMessage extends RpcRequestMessageObject {
  val header = 0x0E
}
