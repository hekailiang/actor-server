package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc._
import java.util.UUID

case class RequestGetDifference(seq: Int, state: Option[UUID]) extends RpcRequestMessage
object RequestGetDifference extends RpcRequestMessageObject {
  val requestType = 0xb
}
