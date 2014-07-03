package com.secretapp.backend.protocol.codecs

import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._
import com.secretapp.backend.protocol.Codecs._
import com.secretapp.backend.data._

trait RequestAuthIdCodec {
  val requestAuthId = provide(RequestAuthId())
}

trait ResponseAuthIdCodec {
  val responseAuthId = int64.pxmap[ResponseAuthId](ResponseAuthId.apply, ResponseAuthId.unapply)
}

trait PingCodec {
  val ping = int64.pxmap[Ping](Ping.apply, Ping.unapply)
}

trait PongCodec {
  val pong = int64.pxmap[Pong](Pong.apply, Pong.unapply)
}

trait NewSessionCodec {
  val newSession = (int64 :: int64).as[NewSession]
}

trait DropCodec {
  val drop = (int64 :: StringCodec.string).as[Drop]
}

trait SendSMSCodeCodec {
  val sendSMSCode = (StringCodec.string :: int64 :: int64).as[SendSMSCode]
}

trait SentSMSCodeCodec {
  val sentSMSCode = (bool :: int64).as[SentSMSCode]
}

trait SignUpCodec {
  val signUp = (StringCodec.string :: int64 :: StringCodec.string :: StringCodec.string :: StringCodec.string :: SexCodec.sex :: BytesCodec.bytes).as[SignUp]
}

trait SignInCodec {
  val signIn = (StringCodec.string :: int64 :: StringCodec.string).as[SignIn]
}

trait AuthorizationCodec {
  val authorization = int64.pxmap[Authorization](Authorization.apply, Authorization.unapply)
}

trait RpcRequestCodec {
  val rpcRequestMessage: Codec[RpcRequestMessage] = discriminated[RpcRequestMessage].by(uint8)
    .\(SendSMSCode.header) { case sms@SendSMSCode(_, _, _) => sms}(sendSMSCode)
    .\(SignIn.header) { case s: SignIn => s } (signIn)
    .\(SignUp.header) { case s: SignUp => s } (signUp)
  val rpcRequest = (int64 :: rpcRequestMessage).as[RpcRequest]
}

trait RpcResponseCodec {
  val rpcResponseMessage: Codec[RpcResponseMessage] = discriminated[RpcResponseMessage].by(uint8)
    .\(SentSMSCode.header) { case sms: SentSMSCode => sms } (sentSMSCode)
    .\(Authorization.header) { case a: Authorization => a } (authorization)
  val rpcResponse = (int64 :: rpcResponseMessage).as[RpcResponse]
}

trait ProtoMessageCodec {
  val protoMessage: Codec[ProtoMessage] = discriminated[ProtoMessage].by(uint8)
    .\(RequestAuthId.header) { case ra: RequestAuthId => ra } (requestAuthId)
    .\(ResponseAuthId.header) { case ra: ResponseAuthId => ra } (responseAuthId)
    .\(Ping.header) { case p: Ping => p } (ping)
    .\(Pong.header) { case p: Pong => p } (pong)
    .\(Drop.header) { case d: Drop => d} (drop)
    .\(NewSession.header) { case s: NewSession => s} (newSession)
}

object ProtoMessageCodec extends ProtoMessageCodec