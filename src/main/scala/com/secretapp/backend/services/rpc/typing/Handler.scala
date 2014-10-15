package com.secretapp.backend.services.rpc.typing

import akka.actor._
import akka.pattern.pipe
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc.typing._
import com.secretapp.backend.data.models.User

class Handler(val sessionActor: ActorRef, val currentUser: User, val typingBrokerRegion: ActorRef, val session: CSession) extends Actor with ActorLogging
    with HandlerService {
  import context.dispatcher

  def receive = {
    case rq @ RpcProtocol.Request(RequestTyping(uid, accessHash, typingType)) =>
      handleRequestTyping(uid, accessHash, typingType) pipeTo sender
    case rq @ RpcProtocol.Request(RequestGroupTyping(chatId, accessHash, typingType)) =>
      handleRequestGroupTyping(chatId, accessHash, typingType) pipeTo sender
  }
}
