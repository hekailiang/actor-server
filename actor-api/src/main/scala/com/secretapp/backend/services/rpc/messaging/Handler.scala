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
  def notImplemented = throw new Exception("Try again later")

  implicit val timeout = Timeout(5.seconds)

  override def preStart(): Unit = {
    log.debug("messaging preStart")
    super.preStart()
  }

  override def postStop(): Unit = {
    log.debug(s"postStop")
    super.postStop()
  }

  def receive = {
    case RpcProtocol.Request(request) =>
      val replyTo = sender()

      handleMessaging
        .orElse(handleGroup)(request)
      //        .orElse(handleHistory)(request)
        .pipeTo(replyTo).future.onFailure {
        case e =>
          log.error(s"Failed to handle messaging request")
          throw e
      }
  }
}
