package com.secretapp.backend.data.json.message.rpc.auth

import com.secretapp.backend.data.json._
import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.json.JsonSpec._
import com.secretapp.backend.data.message.rpc.{RpcResponseMessage, RpcRequestMessage}
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.json.message.struct.JsonFormatsSpec._

class JsonFormatsSpec extends JsonSpec {

  "RpcRequestMessage (de)serializer" should {

    "(de)serialize RequestAuthCode" in {
      val v = RequestAuthCode(1, 2, "apiKey")
      val j = withHeader(RequestAuthCode.header)(
        "phoneNumber" -> "1",
        "appId" -> 2,
        "apiKey" -> "apiKey"
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestSignIn" in {
      val (bitVector, bitVectorJson) = genBitVector
      val v = RequestSignIn(1, "smsHash", "smsCode", bitVector)
      val j = withHeader(RequestSignIn.header)(
        "phoneNumber" -> "1",
        "smsHash" -> "smsHash",
        "smsCode" -> "smsCode",
        "publicKey" -> bitVectorJson
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }


    "(de)serialize RequestSignUp" in {
      val (bitVector, bitVectorJson) = genBitVector
      val v = RequestSignUp(1, "smsHash", "smsCode", "name", bitVector)
      val j = withHeader(RequestSignUp.header)(
        "phoneNumber" -> "1",
        "smsHash" -> "smsHash",
        "smsCode" -> "smsCode",
        "name" -> "name",
        "publicKey" -> bitVectorJson
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

  }

  "RpcResponseMessage (de)serializer" should {

    "(de)serialize ResponseAuth" in {
      val (user, userJson) = genUser
      val v = ResponseAuth(1, user)
      val j = withHeader(ResponseAuth.header)(
        "publicKeyHash" -> "1",
        "user" -> userJson
      )
      testToAndFromJson[RpcResponseMessage](j, v)
    }

    "(de)serialize ResponseAuthCode" in {
      val v = ResponseAuthCode("smsHash", true)
      val j = withHeader(ResponseAuthCode.header)(
        "smsHash" -> "smsHash",
        "isRegistered" -> true
      )
      testToAndFromJson[RpcResponseMessage](j, v)
    }

  }

}
