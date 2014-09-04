package com.secretapp.backend.data.message.rpc.presence

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct.UserId
import scala.collection.immutable

case class UnsubscribeForOnline(users: immutable.Seq[UserId]) extends RpcRequestMessage

object UnsubscribeForOnline extends RpcRequestMessageObject {
  val requestType = 0x21
}
