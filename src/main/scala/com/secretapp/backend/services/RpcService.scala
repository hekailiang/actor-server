package com.secretapp.backend.services

import akka.actor.Actor
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.services.auth._

trait RpcService extends SignService { self: Actor =>
  def handleRpc(p: Package, messageId: Long): PartialFunction[RpcRequest, Unit] = {
    case Request(body) =>
      handleRpcAuth(p, messageId)(body)
    case RequestWithInit(initConnection, body) =>
//      TODO: initConnection
      handleRpcAuth(p, messageId)(body)
  }
}
