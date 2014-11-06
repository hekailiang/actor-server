package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class RequestCreateGroup(randomId: Long, title: String, users: immutable.Seq[struct.UserOutPeer]) extends RpcRequestMessage {
  val header = RequestCreateGroup.header
}

object RequestCreateGroup extends RpcRequestMessageObject {
  val header = 0x41
}
