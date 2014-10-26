package com.secretapp.backend.data.message.rpc

import scodec.bits._

@SerialVersionUID(1L)
case class Error(
  code: Int, tag: String, userMessage: String, canTryAgain: Boolean,
  data: Option[ErrorData] = None
) extends RpcResponse {
  val header = Error.header
}

object Error extends RpcResponseObject {
  val header = 0x02
}
