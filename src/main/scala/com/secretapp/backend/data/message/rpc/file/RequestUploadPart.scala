package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

@SerialVersionUID(1l)
case class RequestUploadPart(config: UploadConfig, offset: Int, data: BitVector) extends RpcRequestMessage {
  val header = RequestUploadPart.requestType
}

object RequestUploadPart extends RpcRequestMessageObject {
  val requestType = 0x14
}
