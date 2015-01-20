package com.secretapp.backend.services.rpc.files

import akka.actor._
import akka.pattern.pipe
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.models
import com.secretapp.backend.persist
import im.actor.server.persist.file.adapter.FileAdapter

class Handler(
  val currentUser: models.User,
  val fileAdapter: FileAdapter
) extends Actor with ActorLogging with HandlerService {

  import context._

  def receive = {
    case rq @ RpcProtocol.Request(RequestStartUpload()) =>
      handleRequestStartUpload() pipeTo sender
    case RpcProtocol.Request(RequestUploadPart(config, offset, data)) =>
      handleRequestUploadFile(config, offset, data) pipeTo sender
    case RpcProtocol.Request(RequestCompleteUpload(config, blocksCount, crc32)) =>
      handleRequestCompleteUpload(config, blocksCount, crc32) pipeTo sender
    case RpcProtocol.Request(RequestGetFile(location, offset, limit)) =>
      handleRequestGetFile(location, offset, limit) pipeTo sender
  }
}
