package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class ResponseFilePart(data: BitVector) extends RpcResponseMessage {
  val header = ResponseFilePart.responseType
}

object ResponseFilePart extends RpcResponseMessageObject {
  val responseType = 0x11
}
