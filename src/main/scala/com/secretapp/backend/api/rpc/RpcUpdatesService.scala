package com.secretapp.backend.api.rpc

import akka.actor._
import akka.pattern.ask
import com.secretapp.backend.api.UpdatesManager
import com.secretapp.backend.data.message.rpc.RpcRequestMessage
import com.secretapp.backend.data.message.update.CommonUpdate
import com.secretapp.backend.data.message.{ update => updateProto, _ }
import com.secretapp.backend.data.message.rpc.Ok
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist.CommonUpdateRecord
import com.secretapp.backend.services._
import com.secretapp.backend.services.transport._
import java.util.UUID
import scala.concurrent.Future
import scala.util.Random
import scalaz.Scalaz._
import scodec.bits._
import scodec.codecs.uuid

trait RpcUpdatesService {
  this: RpcService with PackageManagerService with Actor =>

  import context.dispatcher

  lazy val updatesManager = context.actorOf(Props(
    new UpdatesManager(handleActor, getUser.get.uid, getUser.get.authId)
  ), "updates-manager")

  private var subscribedToUpdates = false

  def handleUpdatesRpc(user: User, p: Package, messageId: Long, rq: RpcRequestMessage) = {
    updatesManager ! RpcProtocol.Request(p, messageId, rq)
  }
}
