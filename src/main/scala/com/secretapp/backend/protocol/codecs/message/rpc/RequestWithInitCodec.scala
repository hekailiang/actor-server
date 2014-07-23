package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.message.rpc.auth._
import com.secretapp.backend.protocol.codecs.message.rpc.update._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.rpc.update._
import scodec.bits._
import scodec.Codec
import scodec.codecs._

object RequestWithInitCodec extends Codec[RequestWithInit] {
  private val codec = (protoPayload(InitConnectionCodec) :: RequestCodec.rpcRequestMessageCodec).as[RequestWithInit]

  def encode(r: RequestWithInit) = codec.encode(r)

  def decode(buf: BitVector) = codec.decode(buf)
}
