package com.secretapp.backend.api

import com.secretapp.backend.data.message.rpc._

trait RpcService {
  def handleRpc : PartialFunction[RpcRequest, Unit] = {
    case r : Request =>
    case r : RequestWithInit =>
  }
}
