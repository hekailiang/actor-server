package com.secretapp.backend.services.rpc.messaging

import akka.actor._
import akka.pattern.pipe
import akka.util.Timeout
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.models
import com.secretapp.backend.persist
import scala.concurrent.Future
import scala.concurrent.duration._

class Handler(
  val updatesBrokerRegion: ActorRef,
  val socialBrokerRegion: ActorRef,
  val fileRecord: persist.File,
  val currentUser: models.User
)(implicit val session: CSession)
  extends Actor with ActorLogging with MessagingHandlers with GroupHandlers with HistoryHandlers {
  import context._

  type RequestMatcher = PartialFunction[RpcRequestMessage, Future[RpcResponse]]

  //val handleHistory: RequestMatcher = ???

  implicit val timeout = Timeout(5.seconds)

  def receive = {
    case RpcProtocol.Request(request) =>
      val replyTo = sender()

      handleMessaging
        .orElse(handleGroup)(request)
//        .orElse(handleHistory)(request)
        .pipeTo(replyTo)
  }
}
