package com.secretapp.backend.services.rpc.messaging

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import com.datastax.driver.core.{ Session => CSession }
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import com.secretapp.backend.api.ApiBrokerService
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.{ Error, RpcResponse, RpcRequestMessage }
import com.secretapp.backend.models.User
import com.secretapp.backend.persist
import scala.concurrent.Future
import scala.concurrent.duration._
import scalaz.Scalaz._
import scalaz._

trait MessagingService {
  this: ApiBrokerService =>

  import context.dispatcher

  lazy val messagingHandler = context.actorOf(
    Props(
      new Handler(
        updatesBrokerRegion,
        socialBrokerRegion,
        fileRecord,
        currentUser.get
      )
    ), "messaging")

  val randomIds = new ConcurrentLinkedHashMap.Builder[Long, Future[RpcResponse]]
    .initialCapacity(10).maximumWeightedCapacity(100).build

  def handleRpcMessaging: PartialFunction[RpcRequestMessage, \/[Throwable, Future[RpcResponse]]] = {
    case r: RequestWithRandomId =>
      authorizedRequest {
        withUniqRandomId(r.randomId)(ask(messagingHandler, RpcProtocol.Request(r)).mapTo[RpcResponse])
      }
    case r @ (
      _: RequestClearChat
        | _: RequestDeleteChat
        | _: RequestCreateGroup
        | _: RequestEditGroupTitle
        | _: RequestInviteUsers
        | _: RequestLeaveGroup
        | _: RequestRemoveUsers
    ) => authorizedRequest {
        ask(messagingHandler, RpcProtocol.Request(r)).mapTo[RpcResponse]
      }
  }

  private def withUniqRandomId(randomId: Long)(f: => Future[RpcResponse]): Future[RpcResponse] = {
    Option(randomIds.get(randomId)) match {
      case Some(resFuture) =>
        resFuture
      case None =>
        val res = f recover {
          case err =>
            Error(400, "INTERNAL_SERVER_ERROR", err.getMessage, true)
        }
        randomIds.put(randomId, res)
        res
    }
  }
}
