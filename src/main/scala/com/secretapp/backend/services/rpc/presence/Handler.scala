package com.secretapp.backend.services.rpc.presence

import akka.actor._
import akka.pattern.pipe
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.data.models.User

class Handler(val sessionActor: ActorRef, val currentUser: User, val presenceBrokerProxy: ActorRef) extends Actor with ActorLogging
    with HandlerService {
  import context.dispatcher

  def receive = {
    case rq @ RpcProtocol.Request(SubscribeForOnline(users)) =>
      handleSubscribeForOnline(users)
    case rq @ RpcProtocol.Request(UnsubscribeForOnline(users)) =>
      handleUnsubscribeForOnline(users)
    case rq @ RpcProtocol.Request(RequestSetOnline(isOnline, timeout)) =>
      handleRequestSetOnline(isOnline, timeout) pipeTo sender
  }
}
