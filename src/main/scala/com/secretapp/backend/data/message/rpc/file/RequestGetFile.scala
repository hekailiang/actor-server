package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._

case class RequestGetFile(fileLocation: FileLocation, offset: Int, limit: Int) extends RpcRequestMessage

object RequestGetFile extends RpcRequestMessageObject {
  val requestType = 0x10
}
