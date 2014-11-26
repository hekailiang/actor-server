package com.secretapp.backend.data.message.rpc.history

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct
import scala.collection.immutable

@SerialVersionUID(1L)
case class RequestDeleteMessage(outPeer: struct.OutPeer, randomIds: immutable.Seq[Long]) extends RpcRequestMessage {
  val header = RequestDeleteMessage.header
}

object RequestDeleteMessage extends RpcRequestMessageObject {
  val header = 0x62
}
