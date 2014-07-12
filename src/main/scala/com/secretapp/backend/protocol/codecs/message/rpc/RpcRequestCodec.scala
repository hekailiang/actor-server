package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._

object RpcRequestCodec extends Codec[RpcRequestBox] {

  private val rpcRequestCodec: Codec[RpcRequestMessage] = discriminated[RpcRequestMessage].by(uint8)
  //    .\(SendSMSCode.header) { case sms@SendSMSCode(_, _, _) => sms}(sendSMSCode)
  //    .\(SignIn.header) { case s: SignIn => s } (signIn)
  //    .\(SignUp.header) { case s: SignUp => s } (signUp)
  private val codec = rpcRequestCodec.pxmap[RpcRequestBox](RpcRequestBox.apply, RpcRequestBox.unapply)

  def encode(r : RpcRequestBox) = codec.encode(r)

  def decode(buf : BitVector) = codec.decode(buf)

}
