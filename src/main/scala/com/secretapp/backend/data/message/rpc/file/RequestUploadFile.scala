package com.secretapp.backend.data.message.rpc.file

import com.google.protobuf.{ ByteString => ProtoByteString }
import com.secretapp.backend.data.message.rpc._

case class RequestUploadFile(config: UploadConfig, blockIndex: Int, data: ProtoByteString) extends RpcRequestMessage

object RequestUploadFile extends RpcRequestMessageObject {
  val requestType = 0x14
}
