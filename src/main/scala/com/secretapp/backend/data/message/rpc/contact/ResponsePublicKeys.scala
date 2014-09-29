package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.rpc.{RpcResponseMessageObject, RpcResponseMessage}
import scala.collection.immutable

case class ResponsePublicKeys(keys: immutable.Seq[PublicKeyResponse]) extends RpcResponseMessage {
  override val header = ResponsePublicKeys.responseType
}

object ResponsePublicKeys extends RpcResponseMessageObject {
  val responseType = 0x18
}
