package com.secretapp.backend.api.rpc

import akka.actor._
import akka.pattern.ask
import com.secretapp.backend.api.{ UpdatesServiceActor, UpdatesBroker, SocialBroker, ApiBrokerService }
import com.secretapp.backend.data.message.rpc.RpcRequestMessage
import com.secretapp.backend.data.message.update.CommonUpdate
import com.secretapp.backend.data.message.{ update => updateProto, _ }
import com.secretapp.backend.data.message.rpc.RpcResponse
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist.CommonUpdateRecord
import com.secretapp.backend.services._
import com.secretapp.backend.protocol.transport._
import java.util.UUID
import scala.concurrent.Future
import scala.util.Random
import scalaz.Scalaz._
import scodec.bits._
import scodec.codecs.uuid

trait RpcUpdatesService {
  this: ApiBrokerService =>

  import context.dispatcher
  import context.system

  lazy val updatesService = context.actorOf(Props(
    new UpdatesServiceActor(context.parent, updatesBrokerRegion, getUser.get.uid, getUser.get.authId)
  ), "updates-service")

  private var subscribedToUpdates = false

  def handleUpdatesRpc(rq: RpcRequestMessage): Future[RpcResponse] = {
    (updatesService ? RpcProtocol.Request(rq)).mapTo[RpcResponse]
  }
}
