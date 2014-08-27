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
    case rq @ RpcProtocol.Request(p, messageId, RequestUploadStart()) =>
      handleRequestUploadStart(p, messageId)()
    case RpcProtocol.Request(p, messageId, RequestUploadFile(config, offset, data)) =>
      handleRequestUploadFile(p, messageId)(config, offset, data)
    case RpcProtocol.Request(p, messageId, RequestCompleteUpload(config, blocksCount, crc32)) =>
      handleRequestCompleteUpload(p, messageId)(config, blocksCount, crc32)
  }
}
