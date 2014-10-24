package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.rpc.{RpcRequestMessageObject, RpcRequestMessage}
import scala.collection.immutable

@SerialVersionUID(1L)
case class RequestPublicKeys(keys: immutable.Seq[PublicKeyRequest]) extends RpcRequestMessage {
  val header = RequestPublicKeys.header
}

object RequestPublicKeys extends RpcRequestMessageObject {
  val header = 0x06
}
