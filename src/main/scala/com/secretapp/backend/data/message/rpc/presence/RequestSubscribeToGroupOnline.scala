package com.secretapp.backend.data.message.rpc.presence

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct.ChatId
import scala.collection.immutable

case class SubscribeToGroupOnline(chatIds: immutable.Seq[ChatId]) extends RpcRequestMessage {
  val header = SubscribeToGroupOnline.requestType
}

object SubscribeToGroupOnline extends RpcRequestMessageObject {
  val requestType = 0x4A
}
