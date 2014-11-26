package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.rpc.{RpcRequestMessageObject, RpcRequestMessage}
import scala.collection.immutable

@SerialVersionUID(1L)
case class RequestGetPublicKeys(keys: immutable.Seq[PublicKeyRequest]) extends RpcRequestMessage {
  val header = RequestGetPublicKeys.header
}

object RequestGetPublicKeys extends RpcRequestMessageObject {
  val header = 0x06
}
