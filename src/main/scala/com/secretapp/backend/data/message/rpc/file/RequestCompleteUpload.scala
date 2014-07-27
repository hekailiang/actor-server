package com.secretapp.backend.data.message.rpc.file

import com.google.protobuf.{ ByteString => ProtoByteString }
import com.secretapp.backend.data.message.rpc._

case class RequestCompleteUpload(config: UploadConfig, blockCount: Int, crc32: Int) extends RpcRequestMessage

object RequestCompleteUpload extends RpcRequestMessageObject {
  val requestType = 0x16
}
