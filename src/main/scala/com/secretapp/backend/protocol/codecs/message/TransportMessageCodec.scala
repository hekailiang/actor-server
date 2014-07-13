package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data.message._
import com.secretapp.backend.protocol.codecs.message.rpc._
import com.secretapp.backend.protocol.codecs.message.update._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

object TransportMessageCodec extends Codec[TransportMessage] {
  private val codec : Codec[TransportMessage] = {
    discriminated[TransportMessage].by(uint8)
      .\(RpcRequestBox.header) { case r : RpcRequestBox => r } (RpcRequestBoxCodec)
      .\(RpcResponseBox.header) { case r : RpcResponseBox => r } (RpcResponseBoxCodec)
      .\(UpdateBox.header) { case u : UpdateBox => u } (UpdateBoxCodec)
      .\(RequestAuthId.header) { case ra : RequestAuthId => ra} (RequestAuthIdCodec)
      .\(ResponseAuthId.header) { case ra : ResponseAuthId => ra } (ResponseAuthIdCodec)
      .\(Ping.header) { case p : Ping => p } (PingCodec)
      .\(Pong.header) { case p : Pong => p } (PongCodec)
      .\(Drop.header) { case d : Drop => d } (DropCodec)
      .\(NewSession.header) { case s : NewSession => s } (NewSessionCodec)
  }

  def encode(tm : TransportMessage) = codec.encode(tm)

  def decode(buf : BitVector) = codec.decode(buf)
}
