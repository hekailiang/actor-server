package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.protocol.codecs.message.rpc.auth._
import com.secretapp.backend.protocol.codecs.message.rpc.update._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.rpc.update._
import scodec.bits._
import scodec.Codec
import scodec.codecs._

object OkCodec extends Codec[Ok] {
  private val rpcResponseMessageCodec: Codec[RpcResponseMessage] = discriminated[RpcResponseMessage].by(uint32)
    .\(CommonUpdate.responseType) { case c : CommonUpdate => c } (CommonUpdateCodec)
    .\(CommonUpdateTooLong.responseType) { case c : CommonUpdateTooLong => c } (CommonUpdateTooLongCodec)
    .\(State.responseType) { case s : State => s } (StateCodec)
    .\(Difference.responseType) { case d : Difference => d } (DifferenceCodec)
    .\(ResponseAuth.responseType) { case r : ResponseAuth => r } (ResponseAuthCodec)
    .\(ResponseAuthCode.responseType) { case r : ResponseAuthCode => r } (ResponseAuthCodeCodec)

  private val codec = rpcResponseMessageCodec.pxmap[Ok](Ok.apply, Ok.unapply)

  def encode(r : Ok) = codec.encode(r)

  def decode(buf : BitVector) = codec.decode(buf)
}
