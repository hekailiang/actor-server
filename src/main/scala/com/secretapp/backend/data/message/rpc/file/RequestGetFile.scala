package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._

case class RequestGetFile(fileLocation: FileLocation, offset: Int, limit: Int) extends RpcRequestMessage {
  override val header = RequestGetFile.requestType
}

object RequestGetFile extends RpcRequestMessageObject {
  override val requestType = 0x10
}
