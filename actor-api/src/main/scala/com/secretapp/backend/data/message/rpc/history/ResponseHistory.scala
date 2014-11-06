package com.secretapp.backend.data.message.rpc.history

import com.secretapp.backend.data.message.rpc._
import scala.collection.immutable

@SerialVersionUID(1L)
case class ResponseHistory(history: immutable.Seq[HistoryMessage]) extends RpcResponseMessage {
  val header = ResponseHistory.header
}

object ResponseHistory extends RpcResponseMessageObject {
  val header = 0x77
}
