package com.secretapp.backend.data.json.message.rpc

import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.json.JsonSpec._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth.ResponseAuthCode
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.data.message.rpc.presence.UnsubscribeFromOnline
import play.api.libs.json.Json
import com.secretapp.backend.data.json.message.struct.JsonFormatsSpec._
import scala.collection.immutable
import scalaz._
import Scalaz._

class JsonFormatsSpec extends JsonSpec {

  "RpcRequest (de)serializer" should {

    "(de)serialize Request" in {
      val (userId, userIdJson) = genUserId

      val v = Request(UnsubscribeFromOnline(immutable.Seq(userId)))
      val j = withHeader(Request.header)(
        "body" -> withHeader(UnsubscribeFromOnline.header)(
          "users" -> Json.arr(userIdJson)
        )
      )
      testToAndFromJson[RpcRequest](j, v)
    }

  }

  "RpcResponse (de)serializer" should {

    "(de)serialize ConnectionNotInitedError" in {
      val v = ConnectionNotInitedError()
      val j = withHeader(ConnectionNotInitedError.header)()
      testToAndFromJson[RpcResponse](j, v)
    }

    "(de)serialize Error" in {
      val (bitVector, bitVectorJson) = genBitVector
      val v = Error(1, "tag", "userMessage", true, bitVector)
      val j = withHeader(Error.header)(
        "code"        -> 1,
        "tag"         -> "tag",
        "userMessage" -> "userMessage",
        "canTryAgain" -> true,
        "errorData"   -> bitVectorJson
      )
      testToAndFromJson[RpcResponse](j, v)
    }

    "(de)serialize FloodWait" in {
      val v = FloodWait(1)
      val j = withHeader(FloodWait.header)(
        "delay"        -> 1
      )
      testToAndFromJson[RpcResponse](j, v)
    }

    "(de)serialize InternalError" in {
      val v = InternalError(true, 1)
      val j = withHeader(InternalError.header)(
        "canTryAgain"   -> true,
        "tryAgainDelay" -> 1
      )
      testToAndFromJson[RpcResponse](j, v)
    }

    "(de)serialize Ok" in {
      val v = Ok(ResponseAuthCode("smsHash", true))
      val j = withHeader(Ok.header)(
        "body" -> withHeader(ResponseAuthCode.header)(
          "smsHash"      -> "smsHash",
          "isRegistered" -> true
        )
      )
      testToAndFromJson[RpcResponse](j, v)
    }

  }

}
