package com.secretapp.backend.data.json.message

import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.json.{UnitFormat, MessageWithHeader}
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.update.contact._
import com.secretapp.backend.data.transport._
import play.api.libs.json._
import com.secretapp.backend.data.json.message.rpc._

trait JsonFormats {

  implicit object updateMessageFormat extends Format[UpdateMessage] {
    override def writes(o: UpdateMessage): JsValue = Json.obj(
      "header" -> o.header,
      "body"   -> (o match {
        case u: SeqUpdate        => seqUpdateFormat.writes(u)
        case u: SeqUpdateTooLong => seqUpdateTooLongFormat.writes(u)
        case u: WeakUpdate       => weakUpdateFormat.writes(u)
        case u: FatSeqUpdate     => fatSeqUpdate.writes(u)
      })
    )

    override def reads(json: JsValue): JsResult[UpdateMessage] = Json.fromJson[MessageWithHeader](json) flatMap {
      case MessageWithHeader(header, body) => header match {
        case SeqUpdate.header        => seqUpdateFormat.reads(body)
        case SeqUpdateTooLong.header => seqUpdateTooLongFormat.reads(body)
        case WeakUpdate.header       => weakUpdateFormat.reads(body)
        case FatSeqUpdate.header     => fatSeqUpdate.reads(body)
      }
    }
  }

  implicit object seqUpdateMessageFormat extends Format[SeqUpdateMessage] {
    override def writes(o: SeqUpdateMessage): JsValue = Json.obj(
      "header" -> o.header,
      "body"   -> (o match {
        case u: AvatarChanged     => avatarChangedFormat.writes(u)
        case u: ContactRegistered => contactRegisteredFormat.writes(u)
        case u: Message           => messageFormat.writes(u)
        case u: MessageRead       => messageReadFormat.writes(u)
        case u: MessageReceived   => messageReceivedFormat.writes(u)
        case u: MessageSent       => messageSentFormat.writes(u)
        case u: NewDevice         => newDeviceFormat.writes(u)
        case u: NewFullDevice     => newFullDeviceFormat.writes(u)
        case u: RemoveDevice      => removeDeviceFormat.writes(u)
      })
    )

    override def reads(json: JsValue): JsResult[SeqUpdateMessage] = Json.fromJson[MessageWithHeader](json) flatMap {
      case MessageWithHeader(header, body) => header match {
        case AvatarChanged.header     => avatarChangedFormat.reads(body)
        case ContactRegistered.header => contactRegisteredFormat.reads(body)
        case Message.header           => messageFormat.reads(body)
        case MessageRead.header       => messageReadFormat.reads(body)
        case MessageReceived.header   => messageReceivedFormat.reads(body)
        case MessageSent.header       => messageSentFormat.reads(body)
        case NewDevice.header         => newDeviceFormat.reads(body)
        case NewFullDevice.header     => newFullDeviceFormat.reads(body)
        case RemoveDevice.header      => removeDeviceFormat.reads(body)
      }
    }
  }

  implicit object weakUpdateMessageFormat extends Format[WeakUpdateMessage] {
    override def writes(o: WeakUpdateMessage): JsValue = Json.obj(
      "header" -> o.header,
      "body"   -> (o match {
        case u: UserLastSeen => userLastSeenFormat.writes(u)
        case u: UserOffline  => userOfflineFormat.writes(u)
        case u: UserOnline   => userOnlineFormat.writes(u)
      })
    )

    override def reads(json: JsValue): JsResult[WeakUpdateMessage] = Json.fromJson[MessageWithHeader](json) flatMap {
      case MessageWithHeader(header, body) => header match {
        case UserLastSeen.header => userLastSeenFormat.reads(body)
        case UserOffline.header  => userOfflineFormat.reads(body)
        case UserOnline.header  => userOnlineFormat.reads(body)
      }
    }
  }

  implicit object transportMessageFormat extends Format[TransportMessage] {
    override def writes(o: TransportMessage): JsValue = Json.obj(
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

  // FIXME: Move to `transport` package
  implicit val messageBoxFormat = Json.format[MessageBox]

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
  implicit val rpcRequestBoxFormat  = Json.format[RpcRequestBox]
  implicit val rpcResponseBoxFormat = Json.format[RpcResponseBox]
  val unsentMessageFormat  = Json.format[UnsentMessage]
  val unsentResponseFormat = Json.format[UnsentResponse]
  implicit val updateBoxFormat      = Json.format[UpdateBox]

}
