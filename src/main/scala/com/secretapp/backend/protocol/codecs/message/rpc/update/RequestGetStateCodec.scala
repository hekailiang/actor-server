package com.secretapp.backend.protocol.codecs.message.rpc.update

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.rpc.update._
import scodec.bits._
import scodec.Codec
import scala.util.Success
import com.secretapp.{ proto => protobuf }

object RequestGetStateCodec extends Codec[RequestGetState] with utils.ProtobufCodec {
  def encode(r: RequestGetState) = {
    val boxed = protobuf.RequestGetState()
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestGetState.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestGetState()) => RequestGetState()
    }
  }
}
