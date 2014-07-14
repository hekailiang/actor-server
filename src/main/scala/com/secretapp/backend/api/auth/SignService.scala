package com.secretapp.backend.api.auth

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._

trait SignService {
  def handleRpcAuth : PartialFunction[RpcRequestMessage, Any] = {
    case RequestAuthCode(phoneNumber, appId, apiKey) =>

    case r : RequestSignIn =>
    case r : RequestSignUp =>
  }
}
