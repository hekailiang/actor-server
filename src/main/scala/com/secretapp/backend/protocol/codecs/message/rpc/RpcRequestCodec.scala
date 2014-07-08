package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.data._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._

object RpcRequestCodec extends Codec[RpcRequest] {

  private val messageCodec: Codec[RpcRequestMessage] = discriminated[RpcRequestMessage].by(uint8)
  //    .\(SendSMSCode.header) { case sms@SendSMSCode(_, _, _) => sms}(sendSMSCode)
  //    .\(SignIn.header) { case s: SignIn => s } (signIn)
  //    .\(SignUp.header) { case s: SignUp => s } (signUp)
  private val codec = messageCodec.pxmap[RpcRequest](RpcRequest.apply, RpcRequest.unapply)

  def encode(r: RpcRequest) = codec.encode(r)

  def decode(buf: BitVector) = codec.decode(buf)

}
