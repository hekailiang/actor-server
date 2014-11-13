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

trait GroupHandlers extends RandomService with UserHelpers with GroupHelpers {
  self: Handler =>

  import context.{ dispatcher, system }
  import UpdatesBroker._

  implicit val session: CSession

  protected def handleRequestCreateGroup(
    randomId: Long,
    title: String,
    users: immutable.Seq[struct.UserOutPeer]
  ): Future[RpcResponse] = ???

  protected def handleRequestEditGroupTitle(
    groupPeer: struct.GroupOutPeer,
    title: String
  ): Future[RpcResponse] = ???

  protected def handleRequestEditGroupAvatar(
    groupPeer: struct.GroupOutPeer,
    fileLocation: models.FileLocation
  ): Future[RpcResponse] = ???

  protected def handleRequestRemoveGroupAvatar(
    groupPeer: struct.GroupOutPeer
  ): Future[RpcResponse] = ???

  protected def handleRequestInviteUsers(
    groupPeer: struct.GroupOutPeer,
    users: immutable.Seq[struct.UserOutPeer]
  ): Future[RpcResponse] = ???

  protected def handleRequestLeaveGroup(
    groupPeer: struct.GroupOutPeer
  ): Future[RpcResponse] = ???

  protected def handleRequestDeleteGroup(
    groupPeer: struct.GroupOutPeer
  ): Future[RpcResponse] = ???

  protected def handleRequestRemoveUsers(
    groupPeer: struct.GroupOutPeer,
    users: immutable.Seq[struct.UserOutPeer]
  ): Future[RpcResponse] = ???
}
