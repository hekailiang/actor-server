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

class JsonFormatsSpec extends Specification {

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

    "(de)serialize UploadConfig" in {
      val v = UploadConfig(BitVector.fromBase64("1234").get)
      val j = Json.obj(
        "serverData" -> "1234"
      )
      testToAndFromJson[UploadConfig](j, v)
    }

    "(de)serialize FileLocation" in {
      val v = FileLocation(1, 2)
      val j = Json.obj(
        "fileId"     -> "1",
        "accessHash" -> "2"
      )
      testToAndFromJson[FileLocation](j, v)
    }

    "(de)serialize ContactToImport" in {
      val v = ContactToImport(1, 2)
      val j = Json.obj(
        "clientPhoneId" -> "1",
        "phoneNumber"   -> "2"
      )
      testToAndFromJson[ContactToImport](j, v)
    }

    "(de)serialize PublicKeyRequest" in {
      val v = PublicKeyRequest(1, 2, 3)
      val j = Json.obj(
        "uid"        -> 1,
        "accessHash" -> "2",
        "keyHash"    -> "3"
      )
      testToAndFromJson[PublicKeyRequest](j, v)
    }

    "(de)serialize EncryptedKey" in {
      val v = EncryptedKey(1, BitVector.fromBase64("1234").get)
      val j = Json.obj(
        "keyHash"         -> "1",
        "aesEncryptedKey" -> "1234"
      )
      testToAndFromJson[EncryptedKey](j, v)
    }

    "(de)serialize EncryptedMessage" in {
      val key = EncryptedKey(1, BitVector.fromBase64("1234").get)
      val v = EncryptedMessage(BitVector.fromBase64("1234").get, immutable.Seq(key))
      val j = Json.obj(
        "aesEncryptedKey" -> "1234",
        "message"         -> "5678"
      )
      testToAndFromJson[EncryptedMessage](j, v)
    }

    "(de)serialize UserId" in {
      val v = UserId(1, 2)
      val j = Json.obj(
        "uid"        -> 1,
        "accessHash" -> "2"
      )
      testToAndFromJson[UserId](j, v)
    }

    "(de)serialize AvatarImage" in {
      val v = AvatarImage(FileLocation(1, 2), 3, 4, 5)
      val j = Json.obj(
        "fileLocation" -> Json.obj(
          "fileId"     -> "1",
          "accessHash" -> "2"
        ),
        "width"        -> 3,
        "height"       -> 4,
        "fileSize"     -> 5
      )
      testToAndFromJson[AvatarImage](j, v)
    }

    "(de)serialize Avatar" in {
      val v = Avatar(
        AvatarImage(FileLocation(1, 2), 3, 4, 5).some,
        AvatarImage(FileLocation(6, 7), 8, 9, 10).some,
        AvatarImage(FileLocation(11, 12), 13, 14, 15).some)
      val j = Json.obj(
        "smallImage" -> Json.obj(
          "fileLocation" -> Json.obj(
            "fileId"     -> "1",
            "accessHash" -> "2"
          ),
          "width"        -> 3,
          "height"       -> 4,
          "fileSize"     -> 5
        ),
        "largeImage" -> Json.obj(
          "fileLocation" -> Json.obj(
            "fileId"     -> "6",
            "accessHash" -> "7"
          ),
          "width"        -> 8,
          "height"       -> 9,
          "fileSize"     -> 10
        ),
        "fullImage" -> Json.obj(
          "fileLocation" -> Json.obj(
            "fileId"     -> "11",
            "accessHash" -> "12"
          ),
          "width"        -> 13,
          "height"       -> 14,
          "fileSize"     -> 15
        )
      )
      testToAndFromJson[Avatar](j, v)
    }

    "(de)serialize Sex" in {
      Json.toJson(Male)                        should be_== (JsString("male"))
      Json.fromJson[Sex](JsString("male")).get should be_== (Male)

      Json.toJson(Female)                        should be_== (JsString("female"))
      Json.fromJson[Sex](JsString("female")).get should be_== (Female)

      Json.toJson(NoSex)                        should be_== (JsString("nosex"))
      Json.fromJson[Sex](JsString("nosex")).get should be_== (NoSex)
    }

    "(de)serialize User" in {
      val v = User(16, 17, "name", Male.some, Set(18), 19, Avatar(
        AvatarImage(FileLocation(1, 2), 3, 4, 5).some,
        AvatarImage(FileLocation(6, 7), 8, 9, 10).some,
        AvatarImage(FileLocation(11, 12), 13, 14, 15).some).some)
      val j = Json.obj(
        "uid"         -> 16,
        "accessHash"  -> "17",
        "name"        -> "name",
        "sex"         -> "male",
        "keyHashes"   -> Json.arr("18"),
        "phoneNumber" -> "19",
        "avatar"      -> Json.obj(
          "smallImage" -> Json.obj(
            "fileLocation" -> Json.obj(
              "fileId"     -> "1",
              "accessHash" -> "2"
            ),
            "width"        -> 3,
            "height"       -> 4,
            "fileSize"     -> 5
          ),
          "largeImage" -> Json.obj(
            "fileLocation" -> Json.obj(
              "fileId"     -> "6",
              "accessHash" -> "7"
            ),
            "width"        -> 8,
            "height"       -> 9,
            "fileSize"     -> 10
          ),
          "fullImage" -> Json.obj(
            "fileLocation" -> Json.obj(
              "fileId"     -> "11",
              "accessHash" -> "12"
            ),
            "width"        -> 13,
            "height"       -> 14,
            "fileSize"     -> 15
          )
        )
      )
      testToAndFromJson[User](j, v)
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
          "useAesKey"  -> true,
          "aesMessage" -> "1234",
          "messages"   -> Json.arr(
            Json.obj(
              "uid"             -> 4,
              "publicKeyHash"   -> "5",
              "aesEncryptedKey" -> "5678",
              "message"         -> "9012"
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
      val v = Error(1, "tag", "userMessage", true)
      val j = Json.obj(
        "header" -> Error.rpcType,
        "body"   -> Json.obj(
          "code"        -> 1,
          "tag"         -> "tag",
          "userMessage" -> "userMessage",
          "canTryAgain" -> true
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

  private def testToAndFromJson[A](json: JsValue, value: A)
                                     (implicit f: Format[A]) = {
    Json.toJson(value)         should be_== (json)
    Json.fromJson[A](json).get should be_== (value)
  }
}