package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

@SerialVersionUID(1l)
case class RequestCreateChat(
  randomId: Long,
  title: String,
  keyHash: BitVector,
  publicKey: BitVector,
  invites: immutable.Seq[InviteUser]
) extends RpcRequestMessage {
  val header = RequestCreateChat.requestType
}

object RequestCreateChat extends RpcRequestMessageObject {
  val requestType = 0x41
}
