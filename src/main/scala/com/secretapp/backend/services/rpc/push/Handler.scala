package com.secretapp.backend.services.rpc.push

import akka.actor.{ActorLogging, Actor}
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc.push.{RequestUnregisterPush, RequestRegisterGooglePush}
import com.secretapp.backend.data.models.User

class Handler(val currentUser: User)
             (implicit val session: CSession)
  extends Actor with ActorLogging with HandlerService {

  override def receive = {
    case RpcProtocol.Request(RequestRegisterGooglePush(projectId, regId)) =>
      handleRequestRegisterGooglePush(projectId, regId)

    case RpcProtocol.Request(RequestUnregisterPush()) =>
      handleRequestUnregisterPush
  }
}
