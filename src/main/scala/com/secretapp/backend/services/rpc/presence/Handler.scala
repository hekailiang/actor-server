package com.secretapp.backend.services.rpc.presence

import akka.actor._
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.data.models.User

class Handler(val handleActor: ActorRef, val currentUser: User, val presenceBrokerProxy: ActorRef) extends Actor with ActorLogging
    with HandlerService {
  def receive = {
    case rq @ RpcProtocol.Request(p, messageId, SubscribeForOnline(users)) =>
      handleSubscribeForOnline(p, messageId)(users)
    case rq @ RpcProtocol.Request(p, messageId, UnsubscribeForOnline(users)) =>
      handleUnsubscribeForOnline(p, messageId)(users)
    case rq @ RpcProtocol.Request(p, messageId, RequestSetOnline(isOnline, timeout)) =>
      handleRequestSetOnline(p, messageId)(isOnline, timeout)
  }
}
