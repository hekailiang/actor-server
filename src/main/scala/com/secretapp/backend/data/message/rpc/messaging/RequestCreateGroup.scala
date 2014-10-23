package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class RequestCreateGroup(
  randomId: Long,
  title: String,
  keyHash: BitVector,
  publicKey: BitVector,
  broadcast: EncryptedRSABroadcast
) extends RpcRequestMessage {
  val header = RequestCreateGroup.requestType
}

object RequestCreateGroup extends RpcRequestMessageObject {
  val requestType = 0x41
}
