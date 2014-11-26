package com.secretapp.backend.data.message.rpc.presence

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct
import scala.collection.immutable

case class RequestSubscribeToGroupOnline(groupIds: immutable.Seq[struct.GroupOutPeer]) extends RpcRequestMessage {
  val header = RequestSubscribeToGroupOnline.header
}

object RequestSubscribeToGroupOnline extends RpcRequestMessageObject {
  val header = 0x4A
}
