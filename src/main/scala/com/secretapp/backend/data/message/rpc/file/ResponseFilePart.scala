package com.secretapp.backend.data.message.rpc.file

import com.google.protobuf.{ ByteString => ProtoByteString }
import com.secretapp.backend.data.message.rpc._

case class ResponseFilePart(data: ProtoByteString) extends RpcResponseMessage

object ResponseFilePart extends RpcResponseMessageObject {
  val responseType = 0x11
}
