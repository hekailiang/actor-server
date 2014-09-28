package com.secretapp.backend.data.json

import com.secretapp.backend.data.message._
import com.secretapp.backend.data.transport.MessageBox
import org.specs2.mutable.Specification
import com.secretapp.backend.data.json.message._
import play.api.libs.json
import play.api.libs.json._
import scala.collection.immutable

class JsonFormatsSpec extends Specification {

  "(de)serializer" should {

    "(de)serialize MessageBox" in {
      val v = MessageBox(1, Ping(2))
      val j = Json.obj(
        "messageId" -> 1,
        "body"      -> Json.obj(
          "header" -> Ping.header,
          "body"   -> Json.obj(
            "randomId" -> 2
          )
        )
      )
      testToAndFromJson[MessageBox](j, v)
    }

    "(de)serialize Container" in {
      val v = Container(immutable.Seq(
        MessageBox(1, Ping(2)),
        MessageBox(3, Pong(4))
      ))
      val j = Json.obj(
        "header" -> Container.header,
        "body"   -> Json.obj(
          "messages" -> Json.arr(
            Json.obj(
              "messageId" -> 1,
              "body"      -> Json.obj(
                "header" -> Ping.header,
                "body"   -> Json.obj(
                  "randomId" -> 2
                )
              )
            ),
            Json.obj(
              "messageId" -> 3,
              "body"      -> Json.obj(
                "header"   -> Pong.header,
                "body"     -> Json.obj(
                  "randomId" -> 4
                )
              )
            )
          )
        )
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize Drop" in {
      val v = Drop(42, "Body")
      val j = Json.obj(
        "header" -> Drop.header,
        "body"   -> Json.obj(
          "messageId" -> 42,
          "message"   -> "Body"
        )
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize MessageAck" in {
      val v = MessageAck(Vector(1, 2, 3))
      val j = Json.obj(
        "header" -> MessageAck.header,
        "body"   -> Json.obj(
          "messageIds" -> Json.arr(1, 2, 3)
        )
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize NewSession" in {
      val v = NewSession(1, 2)
      val j = Json.obj(
        "header" -> NewSession.header,
        "body"   -> Json.obj(
          "sessionId" -> 1,
          "messageId" -> 2
        )
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize Ping" in {
      val v = Ping(1)
      val j = Json.obj(
        "header" -> Ping.header,
        "body"   -> Json.obj(
          "randomId" -> 1
        )
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize Pong" in {
      val v = Pong(1)
      val j = Json.obj(
        "header"   -> Pong.header,
        "body"     -> Json.obj(
          "randomId" -> 1
        )
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize RequestAuthId" in {
      val v = RequestAuthId()
      val j = Json.obj(
        "header" -> RequestAuthId.header,
        "body"   -> Json.obj()
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize RequestResend" in {
      val v = RequestResend(1)
      val j = Json.obj(
        "header" -> RequestResend.header,
        "body"   -> Json.obj(
          "messageId" -> 1
        )
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize ResponseAuthId" in {
      val v = ResponseAuthId(1)
      val j = Json.obj(
        "header" -> ResponseAuthId.header,
        "body"   -> Json.obj(
          "authId" -> 1
        )
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    // "(de)serialize RpcRequestBox" in {}

    // "(de)serialize RpcResponseBox" in {}

    "(de)serialize UnsentMessage" in {
      val v = UnsentMessage(1, 2)
      val j = Json.obj(
        "header" -> UnsentMessage.header,
        "body"   -> Json.obj(
          "messageId" -> 1,
          "length"    -> 2
        )
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize UnsentResponse" in {
      val v = UnsentResponse(1, 2, 3)
      val j = Json.obj(
        "header" -> UnsentResponse.header,
        "body"   -> Json.obj(
          "messageId"        -> 1,
          "requestMessageId" -> 2,
          "length"           -> 3
        )
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    // "(de)serialize UpdateBox" in {}
  }

  private def testToAndFromJson[A](json: JsValue, value: A)
                                     (implicit f: Format[A]) = {
    Json.toJson(value)         should be_== (json)
    Json.fromJson[A](json).get should be_== (value)
  }
}
