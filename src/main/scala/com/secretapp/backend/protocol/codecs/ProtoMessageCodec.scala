package com.secretapp.backend.protocol.codecs

import com.secretapp.backend.data._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

trait RequestAuthIdCodec {
  val requestAuthId = provide(RequestAuthId())
}

object RequestAuthIdCodec extends RequestAuthIdCodec

trait ResponseAuthIdCodec {
  val responseAuthId = int64.pxmap[ResponseAuthId](ResponseAuthId.apply, ResponseAuthId.unapply)
}

object ResponseAuthIdCodec extends ResponseAuthIdCodec

trait PingCodec {
  val ping = int64.pxmap[Ping](Ping.apply, Ping.unapply)
}

object PingCodec extends PingCodec

trait PongCodec {
  val pong = int64.pxmap[Pong](Pong.apply, Pong.unapply)
}

object PongCodec extends PongCodec

trait NewSessionCodec {
  val newSession = (int64 :: int64).as[NewSession]
}

object NewSessionCodec extends NewSessionCodec

trait DropCodec {
  val drop = (int64 :: StringCodec.protoString).as[Drop]
}

object DropCodec extends DropCodec

trait SendSMSCodeCodec {
  val sendSMSCode = (StringCodec.protoString :: int64 :: int64).as[SendSMSCode]
}

object SendSMSCodeCodec extends SendSMSCodeCodec

trait SentSMSCodeCodec {
  val sentSMSCode = (bool :: int64).as[SentSMSCode]
}

object SentSMSCodeCodec extends SentSMSCodeCodec

trait SignUpCodec {
  val signUp = (StringCodec.protoString :: int64 :: StringCodec.protoString :: StringCodec.protoString :: StringCodec.protoString :: SexCodec.sex :: BytesCodec.protoBytes).as[SignUp]
}

object SignUpCodec extends SignUpCodec

trait SignInCodec {
  val signIn = (StringCodec.protoString :: int64 :: StringCodec.protoString).as[SignIn]
}

object SignInCodec extends SignInCodec

trait AuthorizationCodec {
  val authorization = int64.pxmap[Authorization](Authorization.apply, Authorization.unapply)
}

object AuthorizationCodec extends AuthorizationCodec

trait RpcRequestCodec {
  val rpcRequestMessage: Codec[RpcRequestMessage] = discriminated[RpcRequestMessage].by(uint8)
    .\(SendSMSCode.header) { case sms@SendSMSCode(_, _, _) => sms}(sendSMSCode)
    .\(SignIn.header) { case s: SignIn => s } (signIn)
    .\(SignUp.header) { case s: SignUp => s } (signUp)
  val rpcRequest = rpcRequestMessage.pxmap[RpcRequest](RpcRequest.apply, RpcRequest.unapply)
}

object RpcRequestCodec extends RpcRequestCodec

trait RpcResponseCodec {
  val rpcResponseMessage: Codec[RpcResponseMessage] = discriminated[RpcResponseMessage].by(uint8)
    .\(SentSMSCode.header) { case sms: SentSMSCode => sms } (sentSMSCode)
    .\(Authorization.header) { case a: Authorization => a } (authorization)
  val rpcResponse = (int64 :: rpcResponseMessage).as[RpcResponse]
}

object RpcResponseCodec extends RpcResponseCodec

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

trait ProtoMessageWrapperCodec {

  val protoMessageWrapper : Codec[MessageWrapper] = new Codec[MessageWrapper] {

    import ByteConstants._

    def encode(m: MessageWrapper) = {
      for {
        body <- protoMessage.encode(m.body)
        len <- VarIntCodec.encode(body.length / byteSize)
      } yield (BitVector.fromLong(m.messageId) ++ len ++ body)
    }

    def decode(buf: BitVector) = {
      for {
        l <- VarIntCodec.decode(buf.drop(longSize)); (xs, len) = l
        m <- protoMessage.decode(xs.take(len)); (remain, msg) = m
      } yield {
        val messageId = buf.take(longSize).toLong()
        (xs.drop(len), MessageWrapper(1L, Ping(9L)))
      }
    }

  }

}

object ProtoMessageWrapperCodec extends ProtoMessageWrapperCodec
