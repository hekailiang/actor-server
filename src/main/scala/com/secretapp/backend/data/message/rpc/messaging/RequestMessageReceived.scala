package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

case class RequestMessageReceived(uid: Int, randomId: Long, accessHash: Long) extends RpcRequestMessage

object RequestMessageReceived extends RpcRequestMessageObject {
  val requestType = 0x37
}
