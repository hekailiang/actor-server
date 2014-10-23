package com.secretapp.backend.data.message.rpc.presence

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct.GroupId
import scala.collection.immutable

case class SubscribeToGroupOnline(groupIds: immutable.Seq[GroupId]) extends RpcRequestMessage {
  val header = SubscribeToGroupOnline.requestType
}

object SubscribeToGroupOnline extends RpcRequestMessageObject {
  val requestType = 0x4A
}
