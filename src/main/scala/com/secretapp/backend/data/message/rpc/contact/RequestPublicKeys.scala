package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.rpc.{RpcRequestMessageObject, RpcRequestMessage}
import scala.collection.immutable

@SerialVersionUID(1l)
case class RequestPublicKeys(keys: immutable.Seq[PublicKeyRequest]) extends RpcRequestMessage {
  val header = RequestPublicKeys.requestType
}

object RequestPublicKeys extends RpcRequestMessageObject {
  val requestType = 0x06
}
