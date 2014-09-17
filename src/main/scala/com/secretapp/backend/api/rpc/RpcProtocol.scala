package com.secretapp.backend.api.rpc

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.transport.Package

object RpcProtocol {
  sealed trait RpcMessage

  case class Request(body: RpcRequestMessage) extends RpcMessage
  case class Response(body: RpcResponse) extends RpcMessage
}
