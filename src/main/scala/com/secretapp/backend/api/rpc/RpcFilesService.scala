package com.secretapp.backend.api.rpc

import akka.actor._
import com.secretapp.backend.data.message.rpc.RpcRequestMessage
import com.secretapp.backend.data.message.rpc.file.RequestUploadStart
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist.FileBlockRecord
import com.secretapp.backend.services.transport.PackageManagerService

trait RpcFilesService {
  this: PackageManagerService with Actor =>

  import context.dispatcher
  import context.system

  lazy val filesManager = context.actorOf(Props(new FilesManager(handleActor, getUser.get, new FileBlockRecord)), "files")

  def handleRpcFiles(p: Package, messageId: Long): PartialFunction[RpcRequestMessage, Any] = {
    case rq: RequestUploadStart =>
      filesManager ! RpcProtocol.Request(p, messageId, rq)
  }
}
