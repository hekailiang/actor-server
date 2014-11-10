package com.secretapp.backend.data.json.message.rpc.presence

import com.secretapp.backend.data.json._
import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.message.rpc.RpcRequestMessage
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.data.json.message.struct.JsonFormatsSpec._
import play.api.libs.json._

import scala.collection.immutable

class JsonFormatsSpec extends JsonSpec {

  "RpcRequestMessage (de)serializer" should {

    "(de)serialize RequestSetOnline" in {
      val v = RequestSetOnline(true, 1)
      val j = withHeader(RequestSetOnline.header)(
        "isOnline" -> true,
        "timeout"  -> "1"
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize SubscribeToOnline" in {
      val (userId, userIdJson) = genUserId
      val v = SubscribeToOnline(immutable.Seq(userId))
      val j = withHeader(SubscribeToOnline.header)(
        "users" -> Json.arr(userIdJson)
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize UnsubscribeFromOnline" in {
      val (userId, userIdJson) = genUserId
      val v = UnsubscribeFromOnline(immutable.Seq(userId))
      val j = withHeader(UnsubscribeFromOnline.header)(
        "users" -> Json.arr(userIdJson)
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

  }

}
