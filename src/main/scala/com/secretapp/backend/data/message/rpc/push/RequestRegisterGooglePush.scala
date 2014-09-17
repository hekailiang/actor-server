package com.secretapp.backend.data.message.rpc.push

import com.secretapp.backend.data.message.rpc.{RpcRequestMessageObject, RpcRequestMessage}

case class RequestRegisterGooglePush(projectId: Int, regId: String) extends RpcRequestMessage
object RequestRegisterGooglePush extends RpcRequestMessageObject {
  override val requestType = 0x33
}
