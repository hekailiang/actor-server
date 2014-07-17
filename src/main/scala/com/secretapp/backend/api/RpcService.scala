package com.secretapp.backend.api

import akka.actor.Actor
import com.secretapp.backend.api.auth._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.transport.Package

trait RpcService extends SignService { self: Actor =>
  def handleRpc(p: Package, messageId: Long): PartialFunction[RpcRequest, Unit] = {
    case Request(body) =>
      handleRpcAuth(p, messageId)(body)
//    case r: RequestWithInit =>
  }
}
