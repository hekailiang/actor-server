package com.secretapp.backend.data.json.message

import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.rpc.contact.{PublicKeyRequest, ContactToImport, RequestPublicKeys, RequestImportContacts}
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.presence.{UnsubscribeFromOnline, SubscribeToOnline, RequestSetOnline}
import com.secretapp.backend.data.message.rpc.push.{RequestUnregisterPush, RequestRegisterGooglePush}
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.rpc.user.RequestEditAvatar
import com.secretapp.backend.data.message.struct.{AvatarImage, Avatar, User, UserId}
import com.secretapp.backend.data.message.update.UpdateMessage
import com.secretapp.backend.data.transport._
import com.secretapp.backend.data.types.{Male, Female, NoSex, Sex}
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.secretapp.backend.data.json._ // Implicit Long <-> String, keep it below `play.api.libs.json._`

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

  implicit object rpcResponseMessageFormat extends Format[RpcResponseMessage] {
    override def writes(o: RpcResponseMessage): JsValue = ???

    override def reads(json: JsValue): JsResult[RpcResponseMessage] = ???
  }

  implicit object updateMessageFormat extends Format[UpdateMessage] {
    override def writes(o: UpdateMessage): JsValue = ???

    override def reads(json: JsValue): JsResult[UpdateMessage] = ???
  }

  implicit object transportMessageFormat extends Format[TransportMessage] {
    override def writes(o: TransportMessage): JsValue =
      Json.obj(
        "header" -> o.header,
        "body"   -> (o match {
          case c: Container      => containerFormat.writes(c)
          case d: Drop           => dropFormat.writes(d)
          case m: MessageAck     => messageAckFormat.writes(m)
          case n: NewSession     => newSessionFormat.writes(n)
          case p: Ping           => pingFormat.writes(p)
          case p: Pong           => pongFormat.writes(p)
          case r: RequestAuthId  => requestAuthIdFormat.writes(r)
          case r: RequestResend  => requestResendFormat.writes(r)
          case r: ResponseAuthId => responseAuthIdFormat.writes(r)
          case r: RpcRequestBox  => rpcRequestBoxFormat.writes(r)
          case r: RpcResponseBox => rpcResponseBoxFormat.writes(r)
          case m: UnsentMessage  => unsentMessageFormat.writes(m)
          case r: UnsentResponse => unsentResponseFormat.writes(r)
          case u: UpdateBox      => updateBoxFormat.writes(u)
        })
      )

    override def reads(json: JsValue): JsResult[TransportMessage] = Json.fromJson[MessageWithHeader](json) flatMap {
      case MessageWithHeader(header, body) => header match {
        case Container.header      => containerFormat.reads(body)
        case Drop.header           => dropFormat.reads(body)
        case MessageAck.header     => messageAckFormat.reads(body)
        case NewSession.header     => newSessionFormat.reads(body)
        case Ping.header           => pingFormat.reads(body)
        case Pong.header           => pongFormat.reads(body)
        case RequestAuthId.header  => requestAuthIdFormat.reads(body)
        case RequestResend.header  => requestResendFormat.reads(body)
        case ResponseAuthId.header => responseAuthIdFormat.reads(body)
        case RpcRequestBox.header  => rpcRequestBoxFormat.reads(body)
        case RpcResponseBox.header => rpcResponseBoxFormat.reads(body)
        case UnsentMessage.header  => unsentMessageFormat.reads(body)
        case UnsentResponse.header => unsentResponseFormat.reads(body)
        case UpdateBox.header      => updateBoxFormat.reads(body)
      }
    }
  }

  // Misc
  implicit val messageBoxFormat       = Json.format[MessageBox]
  implicit val uploadConfigFormat     = Json.format[UploadConfig]
  implicit val fileLocationFormat     = Json.format[FileLocation]
  implicit val contactToImportFormat  = Json.format[ContactToImport]
  implicit val publicKeyRequestFormat = Json.format[PublicKeyRequest]
  implicit val EncryptedKeyFormat     = Json.format[EncryptedKey]
  implicit val encryptedMessageFormat = Json.format[EncryptedMessage]
  implicit val userIdFormat           = Json.format[UserId]
  implicit val avatarImageFormat      = Json.format[AvatarImage]
  implicit val avatarFormat           = Json.format[Avatar]
  implicit val sexFormat              = new Format[Sex] {
    override def reads(json: JsValue): JsResult[Sex] = json match {
      case JsString("male")   => JsSuccess(Male)
      case JsString("female") => JsSuccess(Female)
      case JsString("nosex")  => JsSuccess(NoSex)
    }

    override def writes(o: Sex): JsValue = o match {
      case Male   => JsString("male")
      case Female => JsString("female")
      case NoSex  => JsString("nosex")
    }
  }
  implicit val userFormat             = Json.format[User]
  //implicit val differenceUpdate       = Json.format[DifferenceUpdate]

  // TransportMessage descendants
  val containerFormat      = Json.format[Container]
  val dropFormat           = Json.format[Drop]
  val messageAckFormat     = Json.format[MessageAck]
  val newSessionFormat     = Json.format[NewSession]
  val pingFormat           = Json.format[Ping]
  val pongFormat           = Json.format[Pong]
  val requestAuthIdFormat  = UnitFormat[RequestAuthId]
  val requestResendFormat  = Json.format[RequestResend]
  val responseAuthIdFormat = Json.format[ResponseAuthId]
  val rpcRequestBoxFormat  = Json.format[RpcRequestBox]
  val rpcResponseBoxFormat = Json.format[RpcResponseBox]
  val unsentMessageFormat  = Json.format[UnsentMessage]
  val unsentResponseFormat = Json.format[UnsentResponse]
  val updateBoxFormat      = Json.format[UpdateBox]

  // RpcRequestMessage descendants
  val requestAuthCodeFormat           = Json.format[RequestAuthCode]
  val requestCompleteUploadFormat     = Json.format[RequestCompleteUpload]
  val requestGetDifferenceFormat      = Json.format[RequestGetDifference]
  val requestGetFileFormat            = Json.format[RequestGetFile]
  val requestGetStateFormat           = UnitFormat[RequestGetState]
  val requestImportContactsFormat     = Json.format[RequestImportContacts]
  val requestMessageReadFormat = Json.format[RequestMessageRead]
  val requestMessageReceivedFormat = Json.format[RequestMessageReceived]
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

  // RpcResponseMessage descendants
  //val differenceFormat                = Json.format[Difference]

  // RpcRequest descendants
  val requestFormat         = Json.format[Request]

  // RpcResponse descendants
  val connectionNotInitedErrorFormat = UnitFormat[ConnectionNotInitedError]
  val errorFormat                    = Json.format[Error]
  val floodWaitFormat                = Json.format[FloodWait]
  val internalErrorFormat            = Json.format[InternalError]
  val okFormat                       = Json.format[Ok]
}
