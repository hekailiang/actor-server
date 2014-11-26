package com.secretapp.backend.services.rpc.presence

import akka.actor._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.{ RpcResponseBox, UpdateBox }
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.rpc.{ResponseVoid, Error, Ok, RpcResponse}
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.data.transport.MTPackage
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.session.SessionProtocol
import com.secretapp.backend.persist
import scala.collection.immutable
import scala.concurrent.Future
import scalaz._
import Scalaz._

trait HandlerService {
  this: Handler =>

  import context.system
  import context.dispatcher
  import PresenceProtocol._

  // TODO: check accessHash
  protected def handleSubscribeToOnline(users: immutable.Seq[struct.UserOutPeer]): Future[RpcResponse] = {
    sessionActor ! SessionProtocol.SubscribeToPresences(users map (_.id))
    Future.successful(Ok(ResponseVoid()))
  }

  protected def handleSubscribeFromOnline(users: immutable.Seq[struct.UserOutPeer]): Future[RpcResponse] = {
    sessionActor ! SessionProtocol.UnsubscribeToPresences(users map (_.id))
    Future.successful(Ok(ResponseVoid()))
  }

  protected def handleSubscribeToGroupOnline(groups: immutable.Seq[struct.GroupOutPeer]): Future[RpcResponse] = {
    sessionActor ! SessionProtocol.SubscribeToGroupPresences(groups map (_.id))
    Future.successful(Ok(ResponseVoid()))
  }

  protected def handleSubscribeFromGroupOnline(groups: immutable.Seq[struct.GroupOutPeer]): Future[RpcResponse] = {
    sessionActor ! SessionProtocol.UnsubscribeFromGroupPresences(groups map (_.id))
    Future.successful(Ok(ResponseVoid()))
  }

  protected def handleRequestSetOnline(isOnline: Boolean, timeout: Long): Future[RpcResponse] = {
    val message = if (isOnline) {
      UserOnline(currentUser.authId, timeout)
    } else {
      UserOffline(currentUser.authId)
    }

    presenceBrokerRegion ! Envelope(currentUser.uid, message)

    for {
      groupIds <- persist.UserGroup.getGroups(currentUser.uid)
    } yield {
      groupIds foreach { groupId =>
        val message = if (isOnline) {
          GroupPresenceProtocol.UserOnline(currentUser.uid, timeout)
        } else {
          GroupPresenceProtocol.UserOffline(currentUser.uid)
        }
        groupPresenceBrokerRegion ! GroupPresenceProtocol.Envelope(groupId, message)
      }
    }

    Future.successful(Ok(ResponseVoid()))
  }
}
