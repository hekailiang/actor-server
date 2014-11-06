package com.secretapp.backend.services.rpc.presence

import akka.actor._
import akka.pattern.pipe
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.models.User

class Handler(
  val sessionActor: ActorRef, val currentUser: User,
  val presenceBrokerRegion: ActorRef, val groupPresenceBrokerRegion: ActorRef)(implicit val session: CSession) extends Actor with ActorLogging
    with HandlerService {
  import context.dispatcher

  def receive = {
    case rq @ RpcProtocol.Request(SubscribeToOnline(users)) =>
      handleSubscribeToOnline(users) pipeTo sender
    case rq @ RpcProtocol.Request(UnsubscribeFromOnline(users)) =>
      handleUnsubscribeFromOnline(users) pipeTo sender
    case rq @ RpcProtocol.Request(SubscribeToGroupOnline(groups)) =>
      handleSubscribeToGroupOnline(groups) pipeTo sender
    case rq @ RpcProtocol.Request(UnsubscribeFromGroupOnline(groups)) =>
      handleUnsubscribeFromGroupOnline(groups) pipeTo sender
    case rq @ RpcProtocol.Request(RequestSetOnline(isOnline, timeout)) =>
      handleRequestSetOnline(isOnline, timeout) pipeTo sender
  }
}
