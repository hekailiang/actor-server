package com.secretapp.backend.data.message.rpc.push

import com.secretapp.backend.data.message.rpc.{RpcResponseMessageObject, RpcResponseMessage}

case class RequestRegisterGooglePush(projectId: Int, token: String) extends RpcResponseMessage
object RequestRegisterGooglePush extends RpcResponseMessageObject {
  override val responseType = 0x33
}
