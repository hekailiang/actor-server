package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

case class RequestUploadPart(config: UploadConfig, offset: Int, data: BitVector) extends RpcRequestMessage {
  override val header = RequestUploadPart.requestType
}

object RequestUploadPart extends RpcRequestMessageObject {
  override val requestType = 0x14
}
