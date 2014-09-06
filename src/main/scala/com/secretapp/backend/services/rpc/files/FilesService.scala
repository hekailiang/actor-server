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

  lazy val filesHandler = context.actorOf(Props(new Handler(handleActor, getUser.get, new FileRecord, clusterProxies.filesCounter)), "files")
  //countersProxies

  def handleRpcFiles(p: Package, messageId: Long): PartialFunction[RpcRequestMessage, Any] = {
    case rq: RequestStartUpload =>
      filesHandler ! RpcProtocol.Request(p, messageId, rq)
    case rq: RequestUploadPart =>
      filesHandler ! RpcProtocol.Request(p, messageId, rq)
    case rq: RequestCompleteUpload =>
      filesHandler ! RpcProtocol.Request(p, messageId, rq)
    case rq: RequestGetFile =>
      filesHandler ! RpcProtocol.Request(p, messageId, rq)
  }
}
