package com.secretapp.backend.services.rpc.files

import akka.actor._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.data.models.User
import com.secretapp.backend.persist.FileRecord

class Handler(
  val handleActor: ActorRef, val currentUser: User,
  val fileRecord: FileRecord, val filesCounterProxy: ActorRef)(implicit val session: CSession)
  extends Actor with ActorLogging with HandlerService {

  def receive = {
    case rq @ RpcProtocol.Request(p, messageId, RequestStartUpload()) =>
      handleRequestUploadStart(p, messageId)()
    case RpcProtocol.Request(p, messageId, RequestUploadPart(config, offset, data)) =>
      handleRequestUploadFile(p, messageId)(config, offset, data)
    case RpcProtocol.Request(p, messageId, RequestCompleteUpload(config, blocksCount, crc32)) =>
      handleRequestCompleteUpload(p, messageId)(config, blocksCount, crc32)
    case RpcProtocol.Request(p, messageId, RequestGetFile(location, offset, limit)) =>
      handleRequestGetFile(p, messageId)(location, offset, limit)
  }
}
