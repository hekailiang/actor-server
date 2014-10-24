package com.secretapp.backend.data.message.rpc.presence

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct.GroupId
import scala.collection.immutable

case class UnsubscribeFromGroupOnline(groupIds: immutable.Seq[GroupId]) extends RpcRequestMessage {
  val header = UnsubscribeFromGroupOnline.requestType
}

object UnsubscribeFromGroupOnline extends RpcRequestMessageObject {
  val requestType = 0x4B
}
