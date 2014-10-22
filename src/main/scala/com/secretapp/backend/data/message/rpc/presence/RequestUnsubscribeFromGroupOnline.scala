package com.secretapp.backend.data.message.rpc.presence

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct.ChatId
import scala.collection.immutable

case class UnsubscribeFromGroupOnline(chatIds: immutable.Seq[ChatId]) extends RpcRequestMessage {
  val header = UnsubscribeFromGroupOnline.header
}

object UnsubscribeFromGroupOnline extends RpcRequestMessageObject {
  val header = 0x4B
}
