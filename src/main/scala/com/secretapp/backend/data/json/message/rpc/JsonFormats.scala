package com.secretapp.backend.data.json.message.rpc

import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.json.{UnitFormat, MessageWithHeader}
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.rpc.contact.{ResponsePublicKeys, ResponseImportedContacts, RequestPublicKeys, RequestImportContacts}
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.data.message.rpc.messaging.{RequestSendGroupMessage, RequestMessageReceived, RequestMessageRead, RequestSendMessage}
import com.secretapp.backend.data.message.rpc.presence.{UnsubscribeFromOnline, SubscribeToOnline, RequestSetOnline}
import com.secretapp.backend.data.message.rpc.push.{RequestUnregisterPush, RequestRegisterGooglePush}
import com.secretapp.backend.data.message.rpc.update.{ResponseSeq, RequestGetState, RequestGetDifference}
import com.secretapp.backend.data.message.rpc.user.{ResponseAvatarChanged, RequestEditAvatar}
import play.api.libs.json._

trait JsonFormats {

  implicit object rpcRequestFormat extends Format[RpcRequest] {
    override def writes(o: RpcRequest): JsValue = Json.obj(
      "header"  -> o.header,
      "body"    -> requestFormat.writes(o.asInstanceOf[Request])
    )

    override def reads(json: JsValue): JsResult[RpcRequest] = Json.fromJson[MessageWithHeader](json) flatMap {
      case MessageWithHeader(header, body) => header match {
        case Request.header         => requestFormat.reads(body)
      }
    }
  }

  implicit object rpcResponseFormat extends Format[RpcResponse] {
    override def writes(o: RpcResponse): JsValue = Json.obj(
      "header" -> o.header,
      "body"   -> (o match {
        case e: ConnectionNotInitedError => connectionNotInitedErrorFormat.writes(e)
        case e: Error                    => errorFormat.writes(e)
        case w: FloodWait                => floodWaitFormat.writes(w)
        case e: InternalError            => internalErrorFormat.writes(e)
        case o: Ok                       => okFormat.writes(o)
      })
    )

    override def reads(json: JsValue): JsResult[RpcResponse] = Json.fromJson[MessageWithHeader](json) flatMap {
      case MessageWithHeader(header, body) => header match {
        case ConnectionNotInitedError.header => connectionNotInitedErrorFormat.reads(body)
        case Error.header                    => errorFormat.reads(body)
        case FloodWait.header                => floodWaitFormat.reads(body)
        case InternalError.header            => internalErrorFormat.reads(body)
        case Ok.header                       => okFormat.reads(body)
      }
    }
  }

  implicit object rpcRequestMessageFormat extends Format[RpcRequestMessage] {
    override def writes(o: RpcRequestMessage): JsValue = Json.obj(
      "header" -> o.header,
      "body"   -> (o match {
        case r: RequestAuthCode           => requestAuthCodeFormat.writes(r)
        case r: RequestCompleteUpload     => requestCompleteUploadFormat.writes(r)
        case r: RequestGetDifference      => requestGetDifferenceFormat.writes(r)
        case r: RequestGetFile            => requestGetFileFormat.writes(r)
        case r: RequestGetState           => requestGetStateFormat.writes(r)
        case r: RequestImportContacts     => requestImportContactsFormat.writes(r)
        case r: RequestMessageRead        => requestMessageReadFormat.writes(r)
        case r: RequestMessageReceived    => requestMessageReceivedFormat.writes(r)
        case r: RequestSendGroupMessage   => requestSendGroupMessageFormat.writes(r)
        case r: RequestPublicKeys         => requestPublicKeysFormat.writes(r)
        case r: RequestRegisterGooglePush => requestRegisterGooglePushFormat.writes(r)
        case r: RequestSendMessage        => requestSendMessageFormat.writes(r)
        case r: RequestEditAvatar         => requestSetAvatarFormat.writes(r)
        case r: RequestSetOnline          => requestSetOnlineFormat.writes(r)
        case r: RequestSignIn             => requestSignInFormat.writes(r)
        case r: RequestSignUp             => requestSignUpFormat.writes(r)
        case r: RequestStartUpload        => requestStartUploadFormat.writes(r)
        case r: RequestUnregisterPush     => requestUnregisterPushFormat.writes(r)
        case r: RequestUploadPart         => requestUploadPartFormat.writes(r)
        case r: SubscribeToOnline         => subscribeForOnlineFormat.writes(r)
        case r: UnsubscribeFromOnline     => unsubscribeForOnlineFormat.writes(r)
      })
    )

    override def reads(json: JsValue): JsResult[RpcRequestMessage] = Json.fromJson[MessageWithHeader](json) flatMap {
      case MessageWithHeader(header, body) => header match {
        case RequestAuthCode.header           => requestAuthCodeFormat.reads(body)
        case RequestCompleteUpload.header     => requestCompleteUploadFormat.reads(body)
        case RequestGetDifference.header      => requestGetDifferenceFormat.reads(body)
        case RequestGetFile.header            => requestGetFileFormat.reads(body)
        case RequestGetState.header           => requestGetStateFormat.reads(body)
        case RequestImportContacts.header     => requestImportContactsFormat.reads(body)
        case RequestMessageRead.header        => requestMessageReadFormat.reads(body)
        case RequestMessageReceived.header    => requestMessageReceivedFormat.reads(body)
        case RequestSendGroupMessage.header   => requestSendGroupMessageFormat.reads(body)
        case RequestPublicKeys.header         => requestPublicKeysFormat.reads(body)
        case RequestRegisterGooglePush.header => requestRegisterGooglePushFormat.reads(body)
        case RequestSendMessage.header        => requestSendMessageFormat.reads(body)
        case RequestEditAvatar.header         => requestSetAvatarFormat.reads(body)
        case RequestSetOnline.header          => requestSetOnlineFormat.reads(body)
        case RequestSignIn.header             => requestSignInFormat.reads(body)
        case RequestSignUp.header             => requestSignUpFormat.reads(body)
        case RequestStartUpload.header        => requestStartUploadFormat.reads(body)
        case RequestUnregisterPush.header     => requestUnregisterPushFormat.reads(body)
        case RequestUploadPart.header         => requestUploadPartFormat.reads(body)
        case SubscribeToOnline.header         => subscribeForOnlineFormat.reads(body)
        case UnsubscribeFromOnline.header     => unsubscribeForOnlineFormat.reads(body)
      }
    }
  }

  implicit object rpcResponseMessageFormat extends Format[RpcResponseMessage] {
    override def writes(o: RpcResponseMessage): JsValue = Json.obj(
      "header" -> o.header,
      "body"   -> (o match {
        case r: ResponseAuth             => responseAuthFormat.writes(r)
        case r: ResponseAuthCode         => responseAuthCodeFormat.writes(r)
        case r: ResponseImportedContacts => responseImportedContactsFormat.writes(r)
        case r: ResponsePublicKeys       => responsePublicKeysFormat.writes(r)
        case r: ResponseFilePart         => responseFilePartFormat.writes(r)
        case r: ResponseUploadCompleted  => responseUploadCompletedFormat.writes(r)
        case r: ResponseUploadStarted    => responseUploadStartedFormat.writes(r)
        case r: ResponseSeq              => responseSeqFormat.writes(r)
        case r: ResponseAvatarChanged    => responseAvatarChanged.writes(r)
      })
    )

    override def reads(json: JsValue): JsResult[RpcResponseMessage] = Json.fromJson[MessageWithHeader](json) flatMap {
      case MessageWithHeader(header, body)         => header match {
        case ResponseAuth.header             => responseAuthFormat.reads(body)
        case ResponseAuthCode.header         => responseAuthCodeFormat.reads(body)
        case ResponseImportedContacts.header => responseImportedContactsFormat.reads(body)
        case ResponsePublicKeys.header       => responsePublicKeysFormat.reads(body)
        case ResponseFilePart.header         => responseFilePartFormat.reads(body)
        case ResponseUploadCompleted.header  => responseUploadCompletedFormat.reads(body)
        case ResponseUploadStarted.header    => responseUploadStartedFormat.reads(body)
        case ResponseSeq.header              => responseSeqFormat.reads(body)
        case ResponseAvatarChanged.header    => responseAvatarChanged.reads(body)
      }
    }
  }

  val requestFormat = Json.format[Request]

  val connectionNotInitedErrorFormat = UnitFormat[ConnectionNotInitedError]
  val errorFormat                    = Json.format[Error]
  val floodWaitFormat                = Json.format[FloodWait]
  val internalErrorFormat            = Json.format[InternalError]
  val okFormat                       = Json.format[Ok]
}
