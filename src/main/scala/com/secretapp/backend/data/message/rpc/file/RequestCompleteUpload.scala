package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._

case class RequestCompleteUpload(config: UploadConfig, blockCount: Int, crc32: Long) extends RpcRequestMessage {
  val header = RequestCompleteUpload.requestType
}

object RequestCompleteUpload extends RpcRequestMessageObject {
  val requestType = 0x16
}
