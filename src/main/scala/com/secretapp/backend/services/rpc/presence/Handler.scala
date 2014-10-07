package com.secretapp.backend.services.rpc.presence

import akka.actor._
import akka.pattern.pipe
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.data.models.User

class Handler(val sessionActor: ActorRef, val currentUser: User, val presenceBrokerRegion: ActorRef) extends Actor with ActorLogging
    with HandlerService {
  import context.dispatcher

  def receive = {
    case rq @ RpcProtocol.Request(SubscribeToOnline(users)) =>
      handleSubscribeToOnline(users) pipeTo sender
    case rq @ RpcProtocol.Request(UnsubscribeFromOnline(users)) =>
      handleUnsubscribeFromOnline(users) pipeTo sender
    case rq @ RpcProtocol.Request(RequestSetOnline(isOnline, timeout)) =>
      handleRequestSetOnline(isOnline, timeout) pipeTo sender
  }
}
