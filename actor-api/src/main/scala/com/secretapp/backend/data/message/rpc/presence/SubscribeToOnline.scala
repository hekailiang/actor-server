package com.secretapp.backend.data.message.rpc.presence

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct.UserId
import scala.collection.immutable

@SerialVersionUID(1L)
case class SubscribeToOnline(users: immutable.Seq[UserId]) extends RpcRequestMessage {
  val header = SubscribeToOnline.header
}

object SubscribeToOnline extends RpcRequestMessageObject {
  val header = 0x20
}
