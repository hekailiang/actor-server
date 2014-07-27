package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._

case class FileUploaded(location: FileLocation) extends RpcResponseMessage

object FileUploaded extends RpcResponseMessageObject {
  val responseType = 0x17
}
