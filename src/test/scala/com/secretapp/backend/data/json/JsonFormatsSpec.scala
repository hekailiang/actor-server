package com.secretapp.backend.data.json

import java.util.UUID

import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth.{RequestSignUp, RequestSignIn, RequestAuthCode}
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.data.message.rpc.messaging.{EncryptedKey, RequestSendMessage, EncryptedMessage}
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
        "body"      -> Json.obj(
          "header" -> Ping.header,
          "body"   -> Json.obj(
            "randomId" -> "2"
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
              "messageId" -> "1",
              "body"      -> Json.obj(
                "header" -> Ping.header,
                "body"   -> Json.obj(
                  "randomId" -> "2"
                )
              )
            ),
            Json.obj(
              "messageId" -> "3",
              "body"      -> Json.obj(
                "header"   -> Pong.header,
                "body"     -> Json.obj(
                  "randomId" -> "4"
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
          "messageId" -> "42",
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
          "messageIds" -> Json.arr("1", "2", "3")
        )
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize NewSession" in {
      val v = NewSession(1, 2)
      val j = Json.obj(
        "header" -> NewSession.header,
        "body"   -> Json.obj(
          "sessionId" -> "1",
          "messageId" -> "2"
        )
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize Ping" in {
      val v = Ping(1)
      val j = Json.obj(
        "header" -> Ping.header,
        "body"   -> Json.obj(
          "randomId" -> "1"
        )
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize Pong" in {
      val v = Pong(1)
      val j = Json.obj(
        "header"   -> Pong.header,
        "body"     -> Json.obj(
          "randomId" -> "1"
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
          "messageId" -> "1"
        )
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    "(de)serialize ResponseAuthId" in {
      val v = ResponseAuthId(1)
      val j = Json.obj(
        "header" -> ResponseAuthId.header,
        "body"   -> Json.obj(
          "authId" -> "1"
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
          "messageId" -> "1",
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
          "messageId"        -> "1",
          "requestMessageId" -> "2",
          "length"           -> 3
        )
      )
      testToAndFromJson[TransportMessage](j, v)
    }

    // "(de)serialize UpdateBox" in {}

    "(de)serialize RequestAuthCode" in {
      val v = RequestAuthCode(1, 2, "apiKey")
      val j = Json.obj(
        "header" -> RequestAuthCode.requestType,
        "body"   -> Json.obj(
          "phoneNumber" -> "1",
          "appId"       -> 2,
          "apiKey"      -> "apiKey"
        )
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestCompleteUpload" in {
      val v = RequestCompleteUpload(UploadConfig(BitVector.fromBase64("1234").get), 1, 2)
      val j = Json.obj(
        "header" -> RequestCompleteUpload.requestType,
        "body"   -> Json.obj(
          "config" -> Json.obj(
            "serverData" -> "1234"
          ),
          "blockCount" -> 1,
          "crc32"      -> "2"
        )
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestGetDifference" in {
      val uuid = UUID.randomUUID()
      val v = RequestGetDifference(1, uuid.some)
      val j = Json.obj(
        "header" -> RequestGetDifference.requestType,
        "body"   -> Json.obj(
          "seq" -> 1,
          "state" -> uuid.toString
        )
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestGetFile" in {
      val v = RequestGetFile(FileLocation(1, 2), 3, 4)
      val j = Json.obj(
        "header"   -> RequestGetFile.requestType,
        "body"     -> Json.obj(
          "fileLocation" -> Json.obj(
            "fileId"     -> "1",
            "accessHash" -> "2"
          ),
          "offset" -> 3,
          "limit"  -> 4
        )
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestGetState" in {
      val v = RequestGetState()
      val j = Json.obj(
        "header" -> RequestGetState.requestType,
        "body"   -> Json.obj()
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestImportContacts" in {
      val v = RequestImportContacts(immutable.Seq(ContactToImport(1, 2)))
      val j = Json.obj(
        "header" -> RequestImportContacts.requestType,
        "body"   -> Json.obj(
          "contacts" -> Json.arr(
            Json.obj(
              "clientPhoneId" -> "1",
              "phoneNumber"   -> "2"
            )
          )
        )
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestPublicKeys" in {
      val v = RequestPublicKeys(immutable.Seq(PublicKeyRequest(1, 2, 3)))
      val j = Json.obj(
        "header" -> RequestPublicKeys.requestType,
        "body"   -> Json.obj(
          "keys" -> Json.arr(
            Json.obj(
              "uid"        -> 1,
              "accessHash" -> "2",
              "keyHash"    -> "3"
            )
          )
        )
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestRegisterGooglePush" in {
      val v = RequestRegisterGooglePush(1, "token")
      val j = Json.obj(
        "header" -> RequestRegisterGooglePush.requestType,
        "body"   -> Json.obj(
          "projectId" -> "1",
          "token"     -> "token"
        )
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestSendMessage" in {
      val key = EncryptedKey(1, BitVector.fromBase64("1234").get)
      val m = EncryptedMessage(BitVector.fromBase64("5678").get, immutable.Seq(key))
      val v = RequestSendMessage(1, 2, 3, m, m.some)
      val j = Json.obj(
        "header" -> RequestSendMessage.requestType,
        "body"   -> Json.obj(
          "uid"        -> 1,
          "accessHash" -> "2",
          "randomId"   -> "3",
          "message"   -> Json.obj(
            "message"         -> "5678",
            "keys"            -> Json.arr(
              Json.obj(
                "keyHash"         -> "1",
                "aesEncryptedKey" -> "1234"
              )
            )
          ),
          "selfMessage" -> Json.obj(
            "message"         -> "5678",
            "keys"            -> Json.arr(
              Json.obj(
                "keyHash"         -> "1",
                "aesEncryptedKey" -> "1234"
              )
            )
          )
        )
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestEditAvatar" in {
      val v = RequestEditAvatar(FileLocation(1, 2))
      val j = Json.obj(
        "header" -> RequestEditAvatar.requestType,
        "body"   -> Json.obj(
          "fileLocation" -> Json.obj(
            "fileId"     -> "1",
            "accessHash" -> "2"
          )
        )
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestSetOnline" in {
      val v = RequestSetOnline(true, 1)
      val j = Json.obj(
        "header" -> RequestSetOnline.requestType,
        "body"   -> Json.obj(
          "isOnline" -> true,
          "timeout"  -> "1"
        )
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestSignIn" in {
      val v = RequestSignIn(1, "smsHash", "smsCode", BitVector.fromBase64("1234").get)
      val j = Json.obj(
        "header" -> RequestSignIn.requestType,
        "body"   -> Json.obj(
          "phoneNumber" -> "1",
          "smsHash"     -> "smsHash",
          "smsCode"     -> "smsCode",
          "publicKey"   -> "1234"
        )
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestSignUp" in {
      val v = RequestSignUp(1, "smsHash", "smsCode", "name", BitVector.fromBase64("1234").get)
      val j = Json.obj(
        "header" -> RequestSignUp.requestType,
        "body"   -> Json.obj(
          "phoneNumber" -> "1",
          "smsHash"     -> "smsHash",
          "smsCode"     -> "smsCode",
          "name"        -> "name",
          "publicKey"   -> "1234"
        )
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestStartUpload" in {
      val v = RequestStartUpload()
      val j = Json.obj(
        "header" -> RequestStartUpload.requestType,
        "body"   -> Json.obj()
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestUnregisterPush" in {
      val v = RequestUnregisterPush()
      val j = Json.obj(
        "header" -> RequestUnregisterPush.requestType,
        "body"   -> Json.obj()
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestUploadPart" in {
      val v = RequestUploadPart(UploadConfig(BitVector.fromBase64("1234").get), 1, BitVector.fromBase64("5678").get)
      val j = Json.obj(
        "header" -> RequestUploadPart.requestType,
        "body"   -> Json.obj(
          "config" -> Json.obj(
            "serverData" -> "1234"
          ),
          "offset" -> 1,
          "data" -> "5678"
        )
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize SubscribeToOnline" in {
      val v = SubscribeToOnline(immutable.Seq(UserId(1, 2)))
      val j = Json.obj(
        "header" -> SubscribeToOnline.requestType,
        "body"   -> Json.obj(
          "users" -> Json.arr(
            Json.obj(
              "uid"        -> 1,
              "accessHash" -> "2"
            )
          )
        )
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize UnsubscribeFromOnline" in {
      val v = UnsubscribeFromOnline(immutable.Seq(UserId(1, 2)))
      val j = Json.obj(
        "header" -> UnsubscribeFromOnline.requestType,
        "body"   -> Json.obj(
          "users" -> Json.arr(
            Json.obj(
              "uid"        -> 1,
              "accessHash" -> "2"
            )
          )
        )
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize Request" in {
      val v = Request(UnsubscribeFromOnline(immutable.Seq(UserId(1, 2))))
      val j = Json.obj(
        "header" -> Request.rpcType,
        "body"   -> Json.obj(
          "body" -> Json.obj(
            "header" -> UnsubscribeFromOnline.requestType,
            "body"   -> Json.obj(
              "users" -> Json.arr(
                Json.obj(
                  "uid"        -> 1,
                  "accessHash" -> "2"
                )
              )
            )
          )
        )
      )
      testToAndFromJson[RpcRequest](j, v)
    }

    "(de)serialize ConnectionNotInitedError" in {
      val v = ConnectionNotInitedError()
      val j = Json.obj(
        "header" -> ConnectionNotInitedError.rpcType,
        "body"   -> Json.obj()
      )
      testToAndFromJson[RpcResponse](j, v)
    }

    "(de)serialize Error" in {
      val v = Error(1, "tag", "userMessage", true, BitVector.fromBase64("1234").get)
      val j = Json.obj(
        "header" -> Error.rpcType,
        "body"   -> Json.obj(
          "code"        -> 1,
          "tag"         -> "tag",
          "userMessage" -> "userMessage",
          "canTryAgain" -> true,
          "errorData"   -> "1234"
        )
      )
      testToAndFromJson[RpcResponse](j, v)
    }

    "(de)serialize FloodWait" in {
      val v = FloodWait(1)
      val j = Json.obj(
        "header" -> FloodWait.rpcType,
        "body"   -> Json.obj(
          "delay"        -> 1
        )
      )
      testToAndFromJson[RpcResponse](j, v)
    }

    "(de)serialize InternalError" in {
      val v = InternalError(true, 1)
      val j = Json.obj(
        "header" -> InternalError.rpcType,
        "body"   -> Json.obj(
          "canTryAgain"   -> true,
          "tryAgainDelay" -> 1
        )
      )
      testToAndFromJson[RpcResponse](j, v)
    }

    /*"(de)serialize Ok" in {
      val v = Ok(true, 1)
      val j = Json.obj(
        "header" -> InternalError.rpcType,
        "body"   -> Json.obj(
          "canTryAgain"   -> true,
          "tryAgainDelay" -> 1
        )
      )
      testToAndFromJson[RpcResponse](j, v)
    }*/
  }
}
