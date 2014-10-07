package com.secretapp.backend.data.json.message

import com.secretapp.backend.data.json._
import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.message.update.SeqUpdateTooLong
import com.secretapp.backend.data.message.{TransportMessage, UpdateBox, RpcResponseBox, RpcRequestBox}
import com.secretapp.backend.data.message.rpc.{ConnectionNotInitedError, Request}
import com.secretapp.backend.data.message.rpc.presence.UnsubscribeFromOnline
import com.secretapp.backend.data.json.message.struct.JsonFormatsSpec._
import play.api.libs.json.Json

import scala.collection.immutable

class JsonFormatsSpec extends JsonSpec {

  "(de)serializer" should {

    "(de)serialize RpcRequestBox" in {
      val (userId, userIdJson) = genUserId
      val v = RpcRequestBox(Request(UnsubscribeFromOnline(immutable.Seq(userId))))
      val j = Json.obj(
        "body" -> withHeader(Request.rpcType)(
          "body" -> withHeader(UnsubscribeFromOnline.requestType)(
            "users" -> Json.arr(userIdJson)
          )
        )
      )
      testToAndFromJson(j, v)
    }

    "(de)serialize RpcResponseBox" in {
      val (userId, userIdJson) = genUserId
      val v = RpcResponseBox(1, ConnectionNotInitedError())
      val j = Json.obj(
        "messageId" -> "1",
        "body" -> withHeader(ConnectionNotInitedError.rpcType)()
      )
      testToAndFromJson(j, v)
    }

  }

  "TransportMessage (de)serializer" should {

    "(de)serialize UpdateBox" in {
      val v = UpdateBox(SeqUpdateTooLong())
      val j = withHeader(UpdateBox.header)(
        "body" -> withHeader(SeqUpdateTooLong.updateHeader)()
      )
      testToAndFromJson[TransportMessage](j, v)
    }

  }

}
