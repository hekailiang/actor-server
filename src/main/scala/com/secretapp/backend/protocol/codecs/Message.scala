package com.secretapp.backend.protocol.codecs

import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._


sealed trait Message

case class RequestAuthId() extends Message
object RequestAuthId {
  val header = 0xf0
  val codec = provide(RequestAuthId())
}

case class ResponseAuthId(authId: Long) extends Message
object ResponseAuthId {
  val header = 0xf1
  val codec = int64.pxmap[ResponseAuthId](ResponseAuthId.apply, ResponseAuthId.unapply)
}

case class Ping(randomId: Long) extends Message
object Ping {
  val header = 0x1
  val codec = int64.pxmap[Ping](Ping.apply, Ping.unapply)
}

case class Pong(randomId: Long) extends Message
object Pong {
  val header = 0x2
  val codec = int64.pxmap[Pong](Pong.apply, Pong.unapply)
}

case class NewSession(sessionId: Long, messageId: Long) extends Message
object NewSession {
  val header = 0xc
  val codec = (int64 :: int64).as[NewSession]
}

case class Drop(messageId: Long, message: String) extends Message
object Drop {
  val header = 0xd
  val codec = (int64 :: String.codec).as[Drop]
}

case class MessageAck(messageIds: Array[Long])
object MessageAck {
  val header = 0x6

}

sealed trait RpcMessage
sealed trait RpcRequestMessage extends RpcMessage
sealed trait RpcResponseMessage extends RpcMessage

case class SendSMSCode(phoneNumber: String, appId: Long, appHash: Long) extends RpcRequestMessage
object SendSMSCode {
  val header = 0x1
  val codec = (String.codec :: int64 :: int64).as[SendSMSCode]
}
case class SentSMSCode(phoneRegistered: Boolean, smsCodeHash: Long) extends RpcResponseMessage
object SentSMSCode {
  val header = 0x2
  val codec = (bool :: int64).as[SentSMSCode]
}
case class SignUp(phoneNumber: String, smsCodeHash: Long, smsCode: String, firstName: String, lastName: String,
                  sex: Sex, photo: BitVector) extends RpcRequestMessage
object SignUp {
  val header = 0x3
  val codec = (String.codec :: int64 :: String.codec :: String.codec :: String.codec :: Sex.codec :: Bytes.codec).as[SignUp]
}
case class SignIn(phoneNumber: String, smsCodeHash: Long, smsCode: String) extends RpcRequestMessage
object SignIn {
  val header = 0x4
  val codec = (String.codec :: int64 :: String.codec).as[SignIn]
}
//  TODO: add user structure
case class Authorization(expiresEpoch: Long) extends RpcResponseMessage
object Authorization {
  val header = 0x5
  val codec = int64.pxmap[Authorization](Authorization.apply, Authorization.unapply)
}

case class RpcRequest(messageId: Long, message: RpcRequestMessage) extends Message
object RpcRequest {
  val header = 0x3
  val codecRequestMessage: Codec[RpcRequestMessage] = discriminated[RpcRequestMessage].by(uint8)
    .\(SendSMSCode.header) { case sms@SendSMSCode(_, _, _) => sms}(SendSMSCode.codec)
    .\(SignIn.header) { case s: SignIn => s } (SignIn.codec)
    .\(SignUp.header) { case s: SignUp => s } (SignUp.codec)
  val codec = (int64 :: codecRequestMessage).as[RpcRequest]
}

case class RpcResponse(messageId: Long, message: RpcResponseMessage) extends Message
object RpcResponse {
  val header = 0x4
  val codecResponseMessage: Codec[RpcResponseMessage] = discriminated[RpcResponseMessage].by(uint8)
    .\(SentSMSCode.header) { case sms: SentSMSCode => sms } (SentSMSCode.codec)
    .\(Authorization.header) { case a: Authorization => a } (Authorization.codec)
  val codec = (int64 :: codecResponseMessage).as[RpcResponse]
}


case class User(firstName: String, lastName: String, sex: Sex, photo: BitVector)



object Message {
  val codec: Codec[Message] = discriminated[Message].by(uint8)
    .\(RequestAuthId.header) { case ra: RequestAuthId => ra } (RequestAuthId.codec)
    .\(ResponseAuthId.header) { case ra: ResponseAuthId => ra } (ResponseAuthId.codec)
    .\(Ping.header) { case p: Ping => p } (Ping.codec)
    .\(Pong.header) { case p: Pong => p } (Pong.codec)
    .\(Drop.header) { case d: Drop => d} (Drop.codec)
    .\(NewSession.header) { case s: NewSession => s} (NewSession.codec)
}
