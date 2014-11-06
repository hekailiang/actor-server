package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class ResponseFilePart(data: BitVector) extends RpcResponseMessage {
  val header = ResponseFilePart.header
}

object ResponseFilePart extends RpcResponseMessageObject {
  val header = 0x11
}
