package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.message.rpc._
import scodec.bits._
import scodec.Codec
import scodec.codecs._

object RpcResponseBoxCodec extends Codec[RpcResponseBox] {
  private val rpcResponseCodec: Codec[RpcResponse] = discriminated[RpcResponse].by(uint8)
    .\(Ok.header) { case r: Ok => r } (OkCodec)
    .\(Error.header) { case e: Error => e } (ErrorCodec)
    .\(ConnectionNotInitedError.header) { case c: ConnectionNotInitedError => c } (ConnectionNotInitedErrorCodec)
    .\(FloodWait.header) { case f: FloodWait => f } (FloodWaitCodec)
    .\(InternalError.header) { case e: InternalError => e } (InternalErrorCodec)
    .\(0, _ => true) { case a: Any => a } (new DiscriminatedErrorCodec("RpcResponseBox"))

  private val codec = (int64 :: protoPayload(rpcResponseCodec)).as[RpcResponseBox]

  def encode(r: RpcResponseBox) = codec.encode(r)

  def decode(buf: BitVector) = codec.decode(buf)
}
