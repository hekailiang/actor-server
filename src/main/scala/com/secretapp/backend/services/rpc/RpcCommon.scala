package com.secretapp.backend.services.rpc

import akka.actor.Actor
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.util.HandleFutureOpt._
import scalaz._
import Scalaz._
import scala.util.{ Success, Failure }

trait RpcCommon { self: Actor with PackageCommon =>
  import context._

  type RpcResult = HandleResult[Error, RpcResponseMessage]

  val internalError = Error(400, "INTERNAL_ERROR", "", true)

  def sendRpcResult(p: Package, messageId: Long)(res: RpcResult): Unit = {
    res onComplete {
      case Success(res) =>
        val message: RpcResponse = res match {
          case \/-(okRes) => Ok(okRes)
          case -\/(errorRes) => errorRes
        }
        sendReply(p.replyWith(messageId, RpcResponseBox(messageId, message)).right)
      case Failure(e) =>
        sendReply(p.replyWith(messageId, RpcResponseBox(messageId, internalError)).right)
        throw e
    }
  }
}
