package com.secretapp.backend.protocol.codecs.message.rpc.update

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.rpc.update._
import scodec.bits._
import scodec.Codec
import scala.util.Success
import com.getsecretapp.{ proto => protobuf }

object RequestGetDifferenceCodec extends Codec[RequestGetDifference] with utils.ProtobufCodec {
  def encode(r: RequestGetDifference) = {
    val boxed = protobuf.RequestGetDifference(r.seq, r.state)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestGetDifference.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestGetDifference(seq, state)) =>
        RequestGetDifference(seq, state)
    }
  }
}
