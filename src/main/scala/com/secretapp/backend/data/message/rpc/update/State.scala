package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc._
import java.util.UUID

case class State(seq: Int, state: Option[UUID]) extends RpcResponseMessage
object State extends RpcResponseMessageObject {
  val responseType = 0xa
}
