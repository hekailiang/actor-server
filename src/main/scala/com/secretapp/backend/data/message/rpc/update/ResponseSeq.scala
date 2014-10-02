package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc._
import java.util.UUID

case class ResponseSeq(seq: Int, state: Option[UUID]) extends RpcResponseMessage {
  override val header = ResponseSeq.responseType
}

object ResponseSeq extends RpcResponseMessageObject {
  override val responseType = 0x48
}
