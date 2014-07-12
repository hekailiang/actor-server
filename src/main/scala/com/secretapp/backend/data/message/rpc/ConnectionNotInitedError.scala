package com.secretapp.backend.data.message.rpc

case class ConnectionNotInitedError() extends RpcResponse
object ConnectionNotInitedError {
  val header = 0x5
}
