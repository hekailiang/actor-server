package com.secretapp.backend.services.rpc.messaging

import akka.actor._
import akka.pattern.pipe
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.models
import com.secretapp.backend.persist
import scala.concurrent.Future

class Handler(
  val updatesBrokerRegion: ActorRef,
  val socialBrokerRegion: ActorRef,
  val fileRecord: persist.File,
  val currentUser: models.User
)(implicit val session: CSession)
  extends Actor with ActorLogging with MessagingHandlers with GroupHandlers with HistoryHandlers {

  type RequestMatcher = PartialFunction[RpcRequestMessage, Future[RpcResponse]]

  val handleMessaging: RequestMatcher = ???
  val handleGroup: RequestMatcher = ???
  val handleHistory: RequestMatcher = ???

  import context._

  def receive = {
    case RpcProtocol.Request(request) =>
      val replyTo = sender()

      handleMessaging
        .orElse(handleGroup)
        .orElse(handleHistory)(request)
        .pipeTo(replyTo)
  }
}
