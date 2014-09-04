package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

case class RequestUploadPart(config: UploadConfig, offset: Int, data: BitVector) extends RpcRequestMessage

object RequestUploadPart extends RpcRequestMessageObject {
  val requestType = 0x14
}
