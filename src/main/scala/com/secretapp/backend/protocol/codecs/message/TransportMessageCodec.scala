package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data.message._
import com.secretapp.backend.protocol.codecs.DiscriminatedErrorCodec
import scodec.bits.BitVector
import scodec.Codec
import scodec.codecs._

object TransportMessageCodec extends Codec[TransportMessage] {
  private val codec: Codec[TransportMessage] = {
    discriminated[TransportMessage].by(uint8)
      .\(RpcRequestBox.header) { case r: RpcRequestBox => r } (RpcRequestBoxCodec)
      .\(RpcResponseBox.header) { case r: RpcResponseBox => r } (RpcResponseBoxCodec)
      .\(UpdateBox.header) { case u: UpdateBox => u } (UpdateBoxCodec)
      .\(Container.header) { case c: Container => c } (ContainerCodec)
      .\(RequestAuthId.header) { case ra: RequestAuthId => ra } (RequestAuthIdCodec)
      .\(ResponseAuthId.header) { case ra: ResponseAuthId => ra } (ResponseAuthIdCodec)
      .\(Ping.header) { case p: Ping => p } (PingCodec)
      .\(Pong.header) { case p: Pong => p } (PongCodec)
      .\(Drop.header) { case d: Drop => d } (DropCodec)
      .\(NewSession.header) { case s: NewSession => s } (NewSessionCodec)
      .\(UnsentMessage.header) { case m: UnsentMessage => m } (UnsentMessageCodec)
      .\(UnsentResponse.header) { case m: UnsentResponse => m } (UnsentResponseCodec)
      .\(RequestResend.header) { case r: RequestResend => r } (RequestResendCodec)
      .\(MessageAck.header) { case m: MessageAck => m } (MessageAckCodec)
      .\(0, _ => true ) { case a: Any => a } (new DiscriminatedErrorCodec("TransportMessage"))
  }

  def encode(tm: TransportMessage) = codec.encode(tm)

  def decode(buf: BitVector) = codec.decode(buf)
}
