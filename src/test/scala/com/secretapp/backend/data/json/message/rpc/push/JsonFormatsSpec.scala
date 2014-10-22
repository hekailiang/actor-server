package com.secretapp.backend.data.json.message.rpc.push

import com.secretapp.backend.data.json._
import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.message.rpc.RpcRequestMessage
import com.secretapp.backend.data.message.rpc.push._

class JsonFormatsSpec extends JsonSpec {

  "RpcRequestMessage (de)serializer" should {

    "(de)serialize RequestRegisterGooglePush" in {
      val v = RequestRegisterGooglePush(1, "token")
      val j = withHeader(RequestRegisterGooglePush.header)(
        "projectId" -> "1",
        "token" -> "token"
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestUnregisterPush" in {
      val v = RequestUnregisterPush()
      val j = withHeader(RequestUnregisterPush.header)()
      testToAndFromJson[RpcRequestMessage](j, v)
    }

  }

}
