package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.rpc.{RpcRequestMessageObject, RpcRequestMessage}
import scala.collection.immutable

case class RequestPublicKeys(keys: immutable.Seq[PublicKeyRequest]) extends RpcRequestMessage {
  override val header = RequestPublicKeys.requestType
}

object RequestPublicKeys extends RpcRequestMessageObject {
  val requestType = 0x6
}
