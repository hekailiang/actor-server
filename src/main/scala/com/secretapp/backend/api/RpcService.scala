package com.secretapp.backend.api

import com.secretapp.backend.data.message.rpc._

trait RpcService {
  def handleRpc : PartialFunction[RpcRequest, Any] = {
    case r : Request =>
    case r : RequestWithInit =>
  }
}
