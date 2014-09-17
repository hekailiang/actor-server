package com.secretapp.backend.services.rpc.push

import akka.actor.{ActorLogging, Actor}
import akka.pattern.pipe
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc.push.{RequestUnregisterPush, RequestRegisterGooglePush}
import com.secretapp.backend.data.models.User

class Handler(val currentUser: User)
             (implicit val session: CSession)
  extends Actor with ActorLogging with HandlerService {
  import context._

  override def receive = {
    case RpcProtocol.Request(RequestRegisterGooglePush(projectId, token)) =>
      handleRequestRegisterGooglePush(projectId, token) pipeTo sender

    case RpcProtocol.Request(RequestUnregisterPush()) =>
      handleRequestUnregisterPush pipeTo sender
  }
}
