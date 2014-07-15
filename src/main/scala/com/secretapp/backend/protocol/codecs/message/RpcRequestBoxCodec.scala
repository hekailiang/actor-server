package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.protocol.codecs.message.rpc._
import scodec.bits._
import scodec.Codec
import scodec.codecs._

object RpcRequestBoxCodec extends Codec[RpcRequestBox] {
  private val rpcRequestCodec: Codec[RpcRequest] = discriminated[RpcRequest].by(uint8)
    .\(Request.rpcType) { case r: Request => r} (RequestCodec)

  private val codec = rpcRequestCodec.pxmap[RpcRequestBox](RpcRequestBox.apply, RpcRequestBox.unapply)

  def encode(r: RpcRequestBox) = codec.encode(r)

  def decode(buf: BitVector) = codec.decode(buf)
}
