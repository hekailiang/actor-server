package com.secretapp.backend.services.rpc.files

import akka.actor._
import com.secretapp.backend.api.ApiHandlerActor
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc.RpcRequestMessage
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist.FileRecord
import com.secretapp.backend.services.transport.PackageManagerService

trait FilesService {
  this: ApiHandlerActor =>

  import context.dispatcher
  import context.system

  lazy val handler = context.actorOf(Props(new Handler(handleActor, getUser.get, new FileRecord, countersProxies.files)), "files")
  //countersProxies

  def handleRpcFiles(p: Package, messageId: Long): PartialFunction[RpcRequestMessage, Any] = {
    case rq: RequestUploadStart =>
      handler ! RpcProtocol.Request(p, messageId, rq)
    case rq: RequestUploadFile =>
      handler ! RpcProtocol.Request(p, messageId, rq)
    case rq: RequestCompleteUpload =>
      handler ! RpcProtocol.Request(p, messageId, rq)
  }
}
