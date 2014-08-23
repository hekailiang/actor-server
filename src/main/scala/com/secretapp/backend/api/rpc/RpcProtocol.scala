package com.secretapp.backend.api.rpc

import com.secretapp.backend.data.message.rpc.RpcRequestMessage
import com.secretapp.backend.data.transport.Package

object RpcProtocol {
  case class Request(p: Package, messageId: Long, body: RpcRequestMessage)
}
