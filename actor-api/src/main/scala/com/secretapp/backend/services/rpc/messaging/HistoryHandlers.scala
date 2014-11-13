package com.secretapp.backend.services.rpc.messaging

import akka.actor._
import akka.pattern.ask
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.{ SocialProtocol, UpdatesBroker }
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.{ Error, Ok, RpcResponse, ResponseAvatarChanged, ResponseVoid }
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.models
import com.secretapp.backend.helpers.{ GroupHelpers, UserHelpers }
import com.secretapp.backend.persist
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.util.{ACL, AvatarUtils}
import java.util.UUID
import scala.collection.immutable
import scala.concurrent.Future
import scala.language.postfixOps
import scalaz.Scalaz._
import scalaz._
import scodec.bits._

trait HistoryHandlers extends RandomService with UserHelpers with GroupHelpers {
  self: Handler =>

  import context.{ dispatcher, system }
  import UpdatesBroker._

  implicit val session: CSession

  protected def handleRequestMessageDelete(
    peer: struct.OutPeer,
    randomId: Long
  ): Future[RpcResponse] = ???

  protected def handleRequestClearChat(
    peer: struct.OutPeer
  ): Future[RpcResponse] = ???

  protected def handleRequestDeleteChat(
    peer: struct.OutPeer
  ): Future[RpcResponse] = ???

  protected def handleRequestLoadDialogs(
    startDate: Long,
    limit: Int
  ): Future[RpcResponse] = ???

  protected def handleRequestLoadHistory(
    peer: struct.OutPeer,
    startDate: Long,
    limit: Int
  ): Future[RpcResponse] = ???
}
