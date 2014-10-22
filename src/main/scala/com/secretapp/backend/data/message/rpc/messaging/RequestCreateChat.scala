package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class RequestCreateChat(
  randomId: Long,
  title: String,
  keyHash: BitVector,
  publicKey: BitVector,
  broadcast: EncryptedRSABroadcast
) extends RpcRequestMessage {
  val header = RequestCreateChat.header
}

object RequestCreateChat extends RpcRequestMessageObject {
  val header = 0x41
}
