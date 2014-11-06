package com.secretapp.backend.data.message.rpc.presence

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct
import scala.collection.immutable

@SerialVersionUID(1L)
case class UnsubscribeFromOnline(users: immutable.Seq[struct.UserOutPeer]) extends RpcRequestMessage {
  val header = UnsubscribeFromOnline.header
}

object UnsubscribeFromOnline extends RpcRequestMessageObject {
  val header = 0x21
}
