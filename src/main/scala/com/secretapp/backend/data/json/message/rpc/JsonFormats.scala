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
      "header"  -> o.rpcType,
      "body"    -> requestFormat.writes(o.asInstanceOf[Request])
    )

    override def reads(json: JsValue): JsResult[RpcRequest] = Json.fromJson[MessageWithHeader](json) flatMap {
      case MessageWithHeader(rpcType, body) => rpcType match {
        case Request.rpcType         => requestFormat.reads(body)
      }
    }
  }

  implicit object rpcResponseFormat extends Format[RpcResponse] {
    override def writes(o: RpcResponse): JsValue = Json.obj(
      "header" -> o.rpcType,
      "body"   -> (o match {
        case e: ConnectionNotInitedError => connectionNotInitedErrorFormat.writes(e)
        case e: Error                    => errorFormat.writes(e)
        case w: FloodWait                => floodWaitFormat.writes(w)
        case e: InternalError            => internalErrorFormat.writes(e)
        case o: Ok                       => okFormat.writes(o)
      })
    )

    override def reads(json: JsValue): JsResult[RpcResponse] = Json.fromJson[MessageWithHeader](json) flatMap {
      case MessageWithHeader(rpcType, body) => rpcType match {
        case ConnectionNotInitedError.rpcType => connectionNotInitedErrorFormat.reads(body)
        case Error.rpcType                    => errorFormat.reads(body)
        case FloodWait.rpcType                => floodWaitFormat.reads(body)
        case InternalError.rpcType            => internalErrorFormat.reads(body)
        case Ok.rpcType                       => okFormat.reads(body)
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
        case RequestAuthCode.requestType           => requestAuthCodeFormat.reads(body)
        case RequestCompleteUpload.requestType     => requestCompleteUploadFormat.reads(body)
        case RequestGetDifference.requestType      => requestGetDifferenceFormat.reads(body)
        case RequestGetFile.requestType            => requestGetFileFormat.reads(body)
        case RequestGetState.requestType           => requestGetStateFormat.reads(body)
        case RequestImportContacts.requestType     => requestImportContactsFormat.reads(body)
        case RequestMessageRead.requestType        => requestMessageReadFormat.reads(body)
        case RequestMessageReceived.requestType    => requestMessageReceivedFormat.reads(body)
        case RequestSendGroupMessage.requestType   => requestSendGroupMessageFormat.reads(body)
        case RequestPublicKeys.requestType         => requestPublicKeysFormat.reads(body)
        case RequestRegisterGooglePush.requestType => requestRegisterGooglePushFormat.reads(body)
        case RequestSendMessage.requestType        => requestSendMessageFormat.reads(body)
        case RequestEditAvatar.requestType         => requestSetAvatarFormat.reads(body)
        case RequestSetOnline.requestType          => requestSetOnlineFormat.reads(body)
        case RequestSignIn.requestType             => requestSignInFormat.reads(body)
        case RequestSignUp.requestType             => requestSignUpFormat.reads(body)
        case RequestStartUpload.requestType        => requestStartUploadFormat.reads(body)
        case RequestUnregisterPush.requestType     => requestUnregisterPushFormat.reads(body)
        case RequestUploadPart.requestType         => requestUploadPartFormat.reads(body)
        case SubscribeToOnline.requestType         => subscribeForOnlineFormat.reads(body)
        case UnsubscribeFromOnline.requestType     => unsubscribeForOnlineFormat.reads(body)
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
        case ResponseAuth.responseType             => responseAuthFormat.reads(body)
        case ResponseAuthCode.responseType         => responseAuthCodeFormat.reads(body)
        case ResponseImportedContacts.responseType => responseImportedContactsFormat.reads(body)
        case ResponsePublicKeys.responseType       => responsePublicKeysFormat.reads(body)
        case ResponseFilePart.responseType         => responseFilePartFormat.reads(body)
        case ResponseUploadCompleted.responseType  => responseUploadCompletedFormat.reads(body)
        case ResponseUploadStarted.responseType    => responseUploadStartedFormat.reads(body)
        case ResponseSeq.responseType              => responseSeqFormat.reads(body)
        case ResponseAvatarChanged.responseType    => responseAvatarChanged.reads(body)
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
