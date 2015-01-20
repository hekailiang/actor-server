package com.secretapp.backend.services.rpc.typing

import akka.actor._
import akka.pattern.pipe
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc.typing._
import com.secretapp.backend.models

class Handler(val sessionActor: ActorRef, val currentUser: models.User, val typingBrokerRegion: ActorRef, val session: CSession) extends Actor with ActorLogging
    with HandlerService {
  import context.dispatcher

  def receive = {
    case RpcProtocol.Request(r: RequestTyping) => r.peer.typ match {
      case models.PeerType.Private => handleRequestTyping(r.peer.id, r.peer.accessHash, r.typingType) pipeTo sender
      case models.PeerType.Group => handleRequestGroupTyping(r.peer.id, r.peer.accessHash, r.typingType) pipeTo sender
    }
  }
}
