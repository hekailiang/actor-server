package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.struct.FileLocation
import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestGetFile(fileLocation: FileLocation, offset: Int, limit: Int) extends RpcRequestMessage {
  val header = RequestGetFile.header
}

object RequestGetFile extends RpcRequestMessageObject {
  val header = 0x10
}
