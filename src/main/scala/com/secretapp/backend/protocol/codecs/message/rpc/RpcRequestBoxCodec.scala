package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.protocol.codecs.message.rpc.auth._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._

object RpcRequestBoxCodec extends Codec[RpcRequestBox] {
  private val rpcRequestCodec: Codec[RpcRequestMessage] = discriminated[RpcRequestMessage].by(uint8)
    .\(RequestAuthCode.requestType) { case r : RequestAuthCode => r} (RequestAuthCodeCodec)
    .\(RequestSignIn.requestType) { case r : RequestSignIn => r} (RequestSignInCodec)
    .\(RequestSignUp.requestType) { case r : RequestSignUp => r} (RequestSignUpCodec)
//    .\(ResponseAuth.requestType) { case r : ResponseAuth => r} (ResponseAuthCodec)
//    .\(RequestAuthCode.requestType) { case r : RequestAuthCode => r} (RequestAuthCodeCodec)

  private val codec = rpcRequestCodec.pxmap[RpcRequestBox](RpcRequestBox.apply, RpcRequestBox.unapply)

  def encode(r : RpcRequestBox) = codec.encode(r)

  def decode(buf : BitVector) = codec.decode(buf)
}
