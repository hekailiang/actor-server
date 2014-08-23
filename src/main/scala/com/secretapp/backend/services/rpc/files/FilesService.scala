package com.secretapp.backend.services.rpc.files

import akka.actor._
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc.RpcRequestMessage
import com.secretapp.backend.data.message.rpc.file.RequestUploadFile
import com.secretapp.backend.data.message.rpc.file.RequestUploadStart
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist.FileBlockRecord
import com.secretapp.backend.services.transport.PackageManagerService

trait FilesService {
  this: PackageManagerService with Actor =>

  import context.dispatcher
  import context.system

  lazy val handler = context.actorOf(Props(new Handler(handleActor, getUser.get, new FileBlockRecord)), "files")

  def handleRpcFiles(p: Package, messageId: Long): PartialFunction[RpcRequestMessage, Any] = {
    case rq: RequestUploadStart =>
      handler ! RpcProtocol.Request(p, messageId, rq)
    case rq: RequestUploadFile =>
      handler ! RpcProtocol.Request(p, messageId, rq)
  }
}
