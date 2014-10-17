package com.secretapp.backend.services.rpc.presence

import akka.actor._
import com.secretapp.backend.data.message.struct.{ ChatId, UserId }
import com.secretapp.backend.data.message.{ RpcResponseBox, UpdateBox }
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.rpc.{ResponseVoid, Error, Ok, RpcResponse}
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.data.transport.MTPackage
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.session.SessionProtocol
import com.secretapp.backend.persist.{ GroupChatUserRecord, UserGroupChatsRecord }
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
  protected def handleSubscribeToOnline(users: immutable.Seq[UserId]): Future[RpcResponse] = {
    log.info(s"handling SubscribeToOnline $users")
    sessionActor ! SessionProtocol.SubscribeToPresences(users map (_.uid))
    Future.successful(Ok(ResponseVoid()))
  }

  protected def handleUnsubscribeFromOnline(users: immutable.Seq[UserId]): Future[RpcResponse] = {
    log.info(s"handling UnsubscribeFromOnline $users")
    sessionActor ! SessionProtocol.UnsubscribeToPresences(users map (_.uid))
    Future.successful(Ok(ResponseVoid()))
  }

  protected def handleSubscribeToGroupOnline(chats: immutable.Seq[ChatId]): Future[RpcResponse] = {
    log.info(s"handling SubscribeToGroupOnline $chats")
    sessionActor ! SessionProtocol.SubscribeToGroupPresences(chats map (_.chatId))
    Future.successful(Ok(ResponseVoid()))
  }

  protected def handleUnsubscribeFromGroupOnline(chats: immutable.Seq[ChatId]): Future[RpcResponse] = {
    log.info(s"handling UnsubscribeFromGroupOnline $chats")
    sessionActor ! SessionProtocol.UnsubscribeFromGroupPresences(chats map (_.chatId))
    Future.successful(Ok(ResponseVoid()))
  }

  protected def handleRequestSetOnline(isOnline: Boolean, timeout: Long): Future[RpcResponse] = {
    log.info(s"Handling RequestSetOnline ${isOnline} ${timeout}")
    val message = if (isOnline) {
      UserOnline(timeout)
    } else {
      UserOffline
    }

    presenceBrokerRegion ! Envelope(currentUser.uid, message)

    for {
      chatIds <- UserGroupChatsRecord.getChats(currentUser.uid)
    } yield {
      chatIds foreach { chatId =>
        val message = if (isOnline) {
          GroupPresenceProtocol.UserOnline(currentUser.uid, timeout)
        } else {
          GroupPresenceProtocol.UserOffline(currentUser.uid)
        }
        groupPresenceBrokerRegion ! GroupPresenceProtocol.Envelope(chatId, message)
      }
    }

    Future.successful(Ok(ResponseVoid()))
  }
}
