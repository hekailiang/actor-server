package com.secretapp.backend.data.message.rpc.presence

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct
import scala.collection.immutable

case class RequestSubscribeFromGroupOnline(groupIds: immutable.Seq[struct.GroupOutPeer]) extends RpcRequestMessage {
  val header = RequestSubscribeFromGroupOnline.header
}

object RequestSubscribeFromGroupOnline extends RpcRequestMessageObject {
  val header = 0x4B
}
