package com.secretapp.backend.data.message.rpc

import scodec.bits._

case class Error(
  code: Int, tag: String, userMessage: String, canTryAgain: Boolean,
  errorData: BitVector = BitVector.empty
) extends RpcResponse {
  val rpcType = Error.rpcType
}

object Error extends RpcResponseObject {
  val rpcType = 0x2
}
