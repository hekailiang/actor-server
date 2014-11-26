package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.rpc.{RpcResponseMessageObject, RpcResponseMessage}
import scala.collection.immutable

@SerialVersionUID(1L)
case class ResponseGetPublicKeys(keys: immutable.Seq[PublicKeyResponse]) extends RpcResponseMessage {
  val header = ResponseGetPublicKeys.header
}

object ResponseGetPublicKeys extends RpcResponseMessageObject {
  val header = 0x18
}
