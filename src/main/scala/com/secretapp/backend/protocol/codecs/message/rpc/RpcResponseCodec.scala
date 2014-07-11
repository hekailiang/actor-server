//package com.secretapp.backend.protocol.codecs.message.rpc
//
//import com.secretapp.backend.data._
//import scodec.bits._
//import scodec.{ Codec, DecodingContext }
//import scodec.codecs._
//
//object RpcResponseCodec extends Codec[RpcResponse] {
//
//  private val messageCodec: Codec[RpcResponseMessage] = discriminated[RpcResponseMessage].by(uint8)
//  //    .\(SentSMSCode.header) { case sms: SentSMSCode => sms } (sentSMSCode)
//  //    .\(Authorization.header) { case a: Authorization => a } (authorization)
//  private val codec = (int64 :: messageCodec).as[RpcResponse]
//
//  def encode(r: RpcResponse) = codec.encode(r)
//
//  def decode(buf: BitVector) = codec.decode(buf)
//
//}
