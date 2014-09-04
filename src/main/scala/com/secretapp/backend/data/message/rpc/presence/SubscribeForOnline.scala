package com.secretapp.backend.data.message.rpc.presence

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct.UserId
import scala.collection.immutable

case class SubscribeForOnline(users: immutable.Seq[UserId]) extends RpcRequestMessage

object SubscribeForOnline extends RpcRequestMessageObject {
  val requestType = 0x20
}
