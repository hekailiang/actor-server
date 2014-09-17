package com.secretapp.backend.services.rpc.push

import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.data.message.rpc.push._
import com.secretapp.backend.data.message.rpc.ResponseVoid

class PushServiceSpec extends RpcSpec {
  import system.dispatcher

  "push service" should {
    "register google push" in {
      implicit val scope = TestScope()

      RequestRegisterGooglePush(3, "token") :~> <~:[ResponseVoid]
    }
  }
}
