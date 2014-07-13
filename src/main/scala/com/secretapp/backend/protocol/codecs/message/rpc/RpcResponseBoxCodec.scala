package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.protocol.codecs.message.rpc.auth._
import com.secretapp.backend.protocol.codecs.message.rpc.update._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._

object RpcResponseBoxCodec extends Codec[RpcResponseBox] {

  private val rpcResponseCodec: Codec[RpcResponseMessage] = discriminated[RpcResponseMessage].by(uint8)
    .\(CommonUpdate.responseType) { case c: CommonUpdate => c } (CommonUpdateCodec)
    .\(ResponseAuth.responseType) { case r: ResponseAuth => r } (ResponseAuthCodec)
    .\(ResponseAuthCode.responseType) { case r: ResponseAuthCode => r } (ResponseAuthCodeCodec)

  private val codec = (int64 :: rpcResponseCodec).as[RpcResponseBox]

  def encode(r : RpcResponseBox) = codec.encode(r)

  def decode(buf : BitVector) = codec.decode(buf)

}
