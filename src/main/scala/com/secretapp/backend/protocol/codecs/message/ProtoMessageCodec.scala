package com.secretapp.backend.protocol.codecs

import com.secretapp.backend.data._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

object ProtoMessageCodec extends Codec[ProtoMessage] {

  private val codec: Codec[ProtoMessage] = {
    discriminated[ProtoMessage].by(uint8)
      .\(RequestAuthId.header) { case ra: RequestAuthId => ra}(message.RequestAuthIdCodec)
      .\(ResponseAuthId.header) { case ra: ResponseAuthId => ra } (message.ResponseAuthIdCodec)
      .\(Ping.header) { case p: Ping => p } (message.PingCodec)
      .\(Pong.header) { case p: Pong => p } (message.PongCodec)
      .\(Drop.header) { case d: Drop => d} (message.DropCodec)
      .\(NewSession.header) { case s: NewSession => s} (message.NewSessionCodec)
  }

  def encode(m: ProtoMessage) = codec.encode(m)

  def decode(buf: BitVector) = codec.decode(buf)

}
