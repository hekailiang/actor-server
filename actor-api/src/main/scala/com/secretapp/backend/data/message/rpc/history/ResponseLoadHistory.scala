package com.secretapp.backend.data.message.rpc.history

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct
import scala.collection.immutable

@SerialVersionUID(1L)
case class ResponseLoadHistory(history: immutable.Seq[HistoryMessage], users: immutable.Seq[struct.User]) extends RpcResponseMessage {
  val header = ResponseLoadHistory.header
}

object ResponseLoadHistory extends RpcResponseMessageObject {
  val header = 0x77
}
