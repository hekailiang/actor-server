package com.secretapp.backend.data.json.message

import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc.{RpcResponse, RpcRequest}
import com.secretapp.backend.data.message.update.UpdateMessage
import com.secretapp.backend.data.transport._
import play.api.libs.json._
import play.api.libs.functional.syntax._

class UnitFormat[A](implicit m: scala.reflect.Manifest[A]) extends Format[A] {
  override def writes(o: A): JsValue = Json.obj()
  override def reads(json: JsValue): JsResult[A] = JsSuccess(m.runtimeClass.newInstance.asInstanceOf[A])
}

trait JsonFormats {

  implicit object rpcRequestFormat extends Format[RpcRequest] {
    override def writes(o: RpcRequest): JsValue = ???

    override def reads(json: JsValue): JsResult[RpcRequest] = ???
  }

  implicit object rpcResponseFormat extends Format[RpcResponse] {
    override def writes(o: RpcResponse): JsValue = ???

    override def reads(json: JsValue): JsResult[RpcResponse] = ???
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

    private case class TransportMessagePrepared(header: Int, body: JsObject)

    private implicit val transportMessagePreparedReads: Reads[TransportMessagePrepared] = (
      (JsPath \ "header").read[Int] ~
      (JsPath \ "body"  ).read[JsObject]
    )(TransportMessagePrepared.apply _)

    override def reads(json: JsValue): JsResult[TransportMessage] = Json.fromJson[TransportMessagePrepared](json) flatMap {
      case TransportMessagePrepared(header, body) => header match {
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

  implicit val messageBoxFormat = Json.format[MessageBox]

  // TransportMessage descendants
  val containerFormat      = Json.format[Container]
  val dropFormat           = Json.format[Drop]
  val messageAckFormat     = Json.format[MessageAck]
  val newSessionFormat     = Json.format[NewSession]
  val pingFormat           = Json.format[Ping]
  val pongFormat           = Json.format[Pong]
  val requestAuthIdFormat  = new UnitFormat[RequestAuthId]
  val requestResendFormat  = Json.format[RequestResend]
  val responseAuthIdFormat = Json.format[ResponseAuthId]
  val rpcRequestBoxFormat  = Json.format[RpcRequestBox]
  val rpcResponseBoxFormat = Json.format[RpcResponseBox]
  val unsentMessageFormat  = Json.format[UnsentMessage]
  val unsentResponseFormat = Json.format[UnsentResponse]
  val updateBoxFormat      = Json.format[UpdateBox]
}
