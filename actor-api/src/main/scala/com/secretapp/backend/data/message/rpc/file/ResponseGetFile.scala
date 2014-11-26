package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class ResponseGetFile(data: BitVector) extends RpcResponseMessage {
  val header = ResponseGetFile.header
}

object ResponseGetFile extends RpcResponseMessageObject {
  val header = 0x11
}
