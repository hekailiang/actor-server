package com.secretapp.backend.protocol.codecs.message.rpc.presence

import com.reactive.messenger.{ api => protobuf }
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scala.util.Success
import scalaz.Scalaz._
import scodec.Codec
import scodec.bits._

object ResponseOnlineCodec extends Codec[ResponseOnline] with utils.ProtobufCodec {
  def encode(r: ResponseOnline) = {
    val boxed = protobuf.ResponseOnline()
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseOnline.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseOnline()) =>
        ResponseOnline()
    }
  }
}
