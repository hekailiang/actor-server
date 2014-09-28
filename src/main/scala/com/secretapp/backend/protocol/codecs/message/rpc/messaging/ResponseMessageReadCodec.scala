package com.secretapp.backend.protocol.codecs.message.rpc.messaging

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import com.reactive.messenger.{ api => protobuf }

object ResponseMessageReadCodec extends Codec[ResponseMessageRead] with utils.ProtobufCodec {
  def encode(r: ResponseMessageRead) = {
    val boxed = protobuf.ResponseMessageRead(r.seq)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseMessageRead.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseMessageRead(seq)) =>
        ResponseMessageRead(seq)
    }
  }
}
