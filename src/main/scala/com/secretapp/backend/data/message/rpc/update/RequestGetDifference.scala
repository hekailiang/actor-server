package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc._
import java.util.UUID

case class RequestGetDifference(seq: Int, state: Option[UUID]) extends RpcRequestMessage {
  override val header = RequestGetDifference.requestType
}
object RequestGetDifference extends RpcRequestMessageObject {
  override val requestType = 0xb
}
