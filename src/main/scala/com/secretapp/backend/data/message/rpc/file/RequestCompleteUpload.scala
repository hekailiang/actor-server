package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestCompleteUpload(config: UploadConfig, blockCount: Int, crc32: Long) extends RpcRequestMessage {
  val header = RequestCompleteUpload.header
}

object RequestCompleteUpload extends RpcRequestMessageObject {
  val header = 0x16
}
