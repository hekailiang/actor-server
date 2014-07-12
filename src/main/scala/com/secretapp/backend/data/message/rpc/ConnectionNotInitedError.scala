package com.secretapp.backend.data.message.rpc

case class ConnectionNotInitedError() extends RpcMessage
object ConnectionNotInitedError {
  val header = 0x5
}
