package com.secretapp.backend.data.json.message.rpc

import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.json.{UnitFormat, MessageWithHeader}
import com.secretapp.backend.data.message.struct._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.json.message.struct._
import com.secretapp.backend.data.message.rpc.auth.{RequestSignUp, RequestSignIn, RequestAuthCode}
import com.secretapp.backend.data.message.rpc.contact.{RequestPublicKeys, RequestImportContacts}
import com.secretapp.backend.data.message.rpc.file.{RequestUploadPart, RequestStartUpload, RequestGetFile, RequestCompleteUpload}
import com.secretapp.backend.data.message.rpc.messaging.RequestSendMessage
import com.secretapp.backend.data.message.rpc.presence.{UnsubscribeFromOnline, SubscribeToOnline, RequestSetOnline}
import com.secretapp.backend.data.message.rpc.push.{RequestUnregisterPush, RequestRegisterGooglePush}
import com.secretapp.backend.data.message.rpc.update.{RequestGetState, RequestGetDifference}
import com.secretapp.backend.data.message.rpc.user.RequestEditAvatar
import play.api.libs.json._
import play.api.libs.functional.syntax._

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

  implicit object rpcRequestMessageFormat extends Format[RpcRequestMessage] {
    override def writes(o: RpcRequestMessage): JsValue = Json.obj(
      "header" -> o.header,
      "body"   -> (o match {
        case c: RequestAuthCode           => requestAuthCodeFormat.writes(c)
        case d: RequestCompleteUpload     => requestCompleteUploadFormat.writes(d)
        case m: RequestGetDifference      => requestGetDifferenceFormat.writes(m)
        case n: RequestGetFile            => requestGetFileFormat.writes(n)
        case p: RequestGetState           => requestGetStateFormat.writes(p)
        case p: RequestImportContacts     => requestImportContactsFormat.writes(p)
        // case RequestMessageRead           => requestMessageRead.reads(json)
        // case RequestMessageReceived       => requestMessageReceived.reads(json)
        case r: RequestPublicKeys         => requestPublicKeysFormat.writes(r)
        case r: RequestRegisterGooglePush => requestRegisterGooglePushFormat.writes(r)
        case r: RequestSendMessage        => requestSendMessageFormat.writes(r)
        case r: RequestEditAvatar         => requestSetAvatarFormat.writes(r)
        case r: RequestSetOnline          => requestSetOnlineFormat.writes(r)
        case m: RequestSignIn             => requestSignInFormat.writes(m)
        case r: RequestSignUp             => requestSignUpFormat.writes(r)
        case u: RequestStartUpload        => requestStartUploadFormat.writes(u)
        case u: RequestUnregisterPush     => requestUnregisterPushFormat.writes(u)
        case u: RequestUploadPart         => requestUploadPartFormat.writes(u)
        case u: SubscribeToOnline         => subscribeForOnlineFormat.writes(u)
        case u: UnsubscribeFromOnline     => unsubscribeForOnlineFormat.writes(u)
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
        //case RequestMessageRead                    => requestMessageRead.reads(body)
        //case RequestMessageReceived                => requestMessageReceived.reads(body)
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

  val requestFormat = Json.format[Request]

  val requestAuthCodeFormat           = Json.format[RequestAuthCode]
  val requestCompleteUploadFormat     = Json.format[RequestCompleteUpload]
  val requestGetDifferenceFormat      = Json.format[RequestGetDifference]
  val requestGetFileFormat            = Json.format[RequestGetFile]
  val requestGetStateFormat           = UnitFormat[RequestGetState]
  val requestImportContactsFormat     = Json.format[RequestImportContacts]
  //val requestMessageReadFormat = Json.format[RequestMessageRead]
  //val requestMessageReceivedFormat = Json.format[RequestMessageReceived]
  val requestPublicKeysFormat         = Json.format[RequestPublicKeys]
  val requestRegisterGooglePushFormat = Json.format[RequestRegisterGooglePush]
  val requestSendMessageFormat        = Json.format[RequestSendMessage]
  val requestSetAvatarFormat          = Json.format[RequestEditAvatar]
  val requestSetOnlineFormat          = Json.format[RequestSetOnline]
  val requestSignInFormat             = Json.format[RequestSignIn]
  val requestSignUpFormat             = Json.format[RequestSignUp]
  val requestStartUploadFormat        = UnitFormat[RequestStartUpload]
  val requestUnregisterPushFormat     = UnitFormat[RequestUnregisterPush]
  val requestUploadPartFormat         = Json.format[RequestUploadPart]
  val subscribeForOnlineFormat        = Json.format[SubscribeToOnline]
  val unsubscribeForOnlineFormat      = Json.format[UnsubscribeFromOnline]
}
