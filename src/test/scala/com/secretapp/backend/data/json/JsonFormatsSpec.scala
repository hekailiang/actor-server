package com.secretapp.backend.data.json

import java.util.UUID

import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth.{RequestSignUp, RequestSignIn, RequestAuthCode}
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.presence.{UnsubscribeFromOnline, SubscribeToOnline, RequestSetOnline}
import com.secretapp.backend.data.message.rpc.push.{RequestUnregisterPush, RequestRegisterGooglePush}
import com.secretapp.backend.data.message.rpc.update.{RequestGetState, RequestGetDifference}
import com.secretapp.backend.data.message.rpc.user.RequestEditAvatar
import com.secretapp.backend.data.message.struct.{User, Avatar, AvatarImage, UserId}
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.data.types.{NoSex, Female, Sex, Male}
import org.specs2.mutable.Specification
import com.secretapp.backend.data.json.message._
import play.api.libs.json
import play.api.libs.json._
import scodec.bits.BitVector
import scala.collection.immutable
import scalaz._
import Scalaz._

class JsonFormatsSpec extends JsonSpec {

  "(de)serializer" should {

    "(de)serialize MessageBox" in {
      val v = MessageBox(1, Ping(2))
      val j = Json.obj(
        "messageId" -> "1",
        "body"      -> withHeader(Ping.header)(
          "randomId" -> "2"
        )
      )
      testToAndFromJson[MessageBox](j, v)
    }

    "(de)serialize Container" in {
      val v = Container(immutable.Seq(
        MessageBox(1, Ping(2)),
        MessageBox(3, Pong(4))
      ))
      val j = withHeader(Container.header)(
        "messages" -> Json.arr(
          Json.obj(
            "messageId" -> "1",
            "body"      -> withHeader(Ping.header)(
              "randomId" -> "2"
            )
          ),
          Json.obj(
            "messageId" -> "3",
            "body"      -> withHeader(Pong.header)(
              "randomId" -> "4"
            )
          )
        )
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize Drop" in {
      val v = Drop(42, "Body")
      val j = withHeader(Drop.header)(
        "messageId" -> "42",
        "message"   -> "Body"
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize MessageAck" in {
      val v = MessageAck(Vector(1, 2, 3))
      val j = withHeader(MessageAck.header)(
        "messageIds" -> Json.arr("1", "2", "3")
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize NewSession" in {
      val v = NewSession(1, 2)
      val j = withHeader(NewSession.header)(
        "sessionId" -> "1",
        "messageId" -> "2"
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize Ping" in {
      val v = Ping(1)
      val j = withHeader(Ping.header)(
        "randomId" -> "1"
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize Pong" in {
      val v = Pong(1)
      val j = withHeader(Pong.header)(
        "randomId" -> "1"
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize RequestAuthId" in {
      val v = RequestAuthId()
      val j = withHeader(RequestAuthId.header)()
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize RequestResend" in {
      val v = RequestResend(1)
      val j = withHeader(RequestResend.header)(
        "messageId" -> "1"
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize ResponseAuthId" in {
      val v = ResponseAuthId(1)
      val j = withHeader(ResponseAuthId.header)(
        "authId" -> "1"
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize UnsentMessage" in {
      val v = UnsentMessage(1, 2)
      val j = withHeader(UnsentMessage.header)(
        "messageId" -> "1",
        "length"    -> 2
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize UnsentResponse" in {
      val v = UnsentResponse(1, 2, 3)
      val j = withHeader(UnsentResponse.header)(
        "messageId"        -> "1",
        "requestMessageId" -> "2",
        "length"           -> 3
      )
      testToAndFromJson[TransportMessage](j, v)
    }

  }
}
