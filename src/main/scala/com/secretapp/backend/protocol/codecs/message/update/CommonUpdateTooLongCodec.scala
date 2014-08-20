package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import com.secretapp.{ proto => protobuf }

object CommonUpdateTooLongCodec extends Codec[CommonUpdateTooLong] with utils.ProtobufCodec {
  def encode(u: CommonUpdateTooLong) = {
    val boxed = protobuf.CommonUpdateTooLong()
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.CommonUpdateTooLong.parseFrom(buf.toByteArray)) {
      case Success(protobuf.CommonUpdateTooLong()) => CommonUpdateTooLong()
    }
  }
}
