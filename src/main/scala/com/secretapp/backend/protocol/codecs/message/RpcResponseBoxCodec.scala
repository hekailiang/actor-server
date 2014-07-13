package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.protocol.codecs.message.rpc._
import scodec.bits._
import scodec.Codec
import scodec.codecs._

object RpcResponseBoxCodec extends Codec[RpcResponseBox] {

  private val rpcResponseCodec: Codec[RpcResponse] = discriminated[RpcResponse].by(uint8)
    .\(Ok.rpcType) { case r : Ok => r } (OkCodec)

  private val codec = (int64 :: rpcResponseCodec).as[RpcResponseBox]

  def encode(r : RpcResponseBox) = codec.encode(r)

  def decode(buf : BitVector) = codec.decode(buf)

}
