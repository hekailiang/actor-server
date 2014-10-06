package com.secretapp.backend.data.json.message

import com.secretapp.backend.data.json.CommonJsonFormats._
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
import com.secretapp.backend.data.json.message.rpc._

trait JsonFormats {

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

  // RpcResponseMessage descendants
  //val differenceFormat                = Json.format[Difference]

  // RpcResponse descendants
  val connectionNotInitedErrorFormat = UnitFormat[ConnectionNotInitedError]
  val errorFormat                    = Json.format[Error]
  val floodWaitFormat                = Json.format[FloodWait]
  val internalErrorFormat            = Json.format[InternalError]
  val okFormat                       = Json.format[Ok]
}
