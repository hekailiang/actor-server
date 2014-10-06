package com.secretapp.backend.data.json.message.rpc

import java.util.UUID

import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.json.JsonSpec._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth.{RequestSignUp, RequestSignIn, RequestAuthCode}
import com.secretapp.backend.data.message.rpc.contact.{PublicKeyRequest, RequestPublicKeys, ContactToImport, RequestImportContacts}
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.data.message.rpc.messaging.{RequestSendMessage, EncryptedMessage, EncryptedKey}
import com.secretapp.backend.data.message.rpc.presence.{SubscribeToOnline, RequestSetOnline, UnsubscribeFromOnline}
import com.secretapp.backend.data.message.rpc.push.{RequestUnregisterPush, RequestRegisterGooglePush}
import com.secretapp.backend.data.message.rpc.update.{RequestGetState, RequestGetDifference}
import com.secretapp.backend.data.message.rpc.user.RequestEditAvatar
import com.secretapp.backend.data.message.struct.UserId
import play.api.libs.json.Json
import com.secretapp.backend.data.json.message.struct.JsonFormatsSpec._
import scodec.bits.BitVector
import scala.collection.immutable
import scalaz._
import Scalaz._

class JsonFormatsSpec extends JsonSpec {

  "RpcRequest (de)serializer" should {

    "(de)serialize Request" in {
      val (userId, userIdJson) = genUserId

      val v = Request(UnsubscribeFromOnline(immutable.Seq(userId)))
      val j = withHeader(Request.rpcType)(
        "body" -> withHeader(UnsubscribeFromOnline.requestType)(
          "users" -> Json.arr(userIdJson)
        )
      )
      testToAndFromJson[RpcRequest](j, v)
    }

  }

  "RpcResponse (de)serializer" should {

    "(de)serialize ConnectionNotInitedError" in {
      val v = ConnectionNotInitedError()
      val j = withHeader(ConnectionNotInitedError.rpcType)()
      testToAndFromJson[RpcResponse](j, v)
    }

    "(de)serialize Error" in {
      val (bitVector, bitVectorJson) = genBitVector
      val v = Error(1, "tag", "userMessage", true, bitVector)
      val j = withHeader(Error.rpcType)(
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
      val j = withHeader(FloodWait.rpcType)(
        "delay"        -> 1
      )
      testToAndFromJson[RpcResponse](j, v)
    }

    "(de)serialize InternalError" in {
      val v = InternalError(true, 1)
      val j = withHeader(InternalError.rpcType)(
        "canTryAgain"   -> true,
        "tryAgainDelay" -> 1
      )
      testToAndFromJson[RpcResponse](j, v)
    }

    "(de)serialize Ok" in {
      true should_== false
    }.pendingUntilFixed("Not implemented yet")

  }

  "RpcRequestMessage (de)serializer" should {

    "(de)serialize RequestRegisterGooglePush" in {
      val v = RequestRegisterGooglePush(1, "token")
      val j = withHeader(RequestRegisterGooglePush.requestType)(
        "projectId" -> "1",
        "token"     -> "token"
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }

    "(de)serialize RequestEditAvatar" in {
      val v = RequestEditAvatar(FileLocation(1, 2))
      val j = withHeader(RequestEditAvatar.requestType)(
        "fileLocation" -> Json.obj(
          "fileId"     -> "1",
          "accessHash" -> "2"
        )
      )
      testToAndFromJson[RpcRequestMessage](j, v)
    }.pendingUntilFixed

    "(de)serialize RequestUnregisterPush" in {
      val v = RequestUnregisterPush()
      val j = withHeader(RequestUnregisterPush.requestType)()
      testToAndFromJson[RpcRequestMessage](j, v)
    }

  }
}
