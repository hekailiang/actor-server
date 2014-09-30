package com.secretapp.backend.services.rpc.presence

import akka.actor._
import com.secretapp.backend.data.message.struct.UserId
import com.secretapp.backend.data.message.{ RpcResponseBox, UpdateBox }
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.rpc.{ResponseVoid, Error, Ok, RpcResponse}
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.data.transport.MTPackage
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.session.SessionProtocol
import scala.collection.immutable
import scala.concurrent.Future
import scalaz._
import Scalaz._

trait HandlerService {
  this: Handler =>

  import context.system
  import context.dispatcher
  import PresenceProtocol._
  import SessionProtocol._

  // TODO: check accessHash
  protected def handleSubscribeToOnline(users: immutable.Seq[UserId]): Future[RpcResponse] = {
    log.info(s"handling SubscribeToOnline $users")
    sessionActor ! SubscribeToPresences(users map (_.uid))
    Future.successful(Ok(ResponseVoid()))
  }

  protected def handleUnsubscribeFromOnline(users: immutable.Seq[UserId]): Future[RpcResponse] = {
    log.info(s"handling UnsubscribeFromOnline $users")
    sessionActor ! UnsubscribeToPresences(users map (_.uid))
    Future.successful(Ok(ResponseVoid()))
  }

  protected def handleRequestSetOnline(isOnline: Boolean, timeout: Long): Future[RpcResponse] = {
    log.info(s"Handling RequestSetOnline ${isOnline} ${timeout}")
    val message = if (isOnline) {
      UserOnline(currentUser.uid, timeout)
    } else {
      UserOffline(currentUser.uid)
    }

    presenceBrokerProxy ! message

    Future.successful(Ok(ResponseVoid()))
  }
}
