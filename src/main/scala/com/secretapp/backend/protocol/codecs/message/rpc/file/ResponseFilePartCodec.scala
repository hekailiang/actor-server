package com.secretapp.backend.protocol.codecs.message.rpc.file

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object ResponseFilePartCodec extends Codec[ResponseFilePart] with utils.ProtobufCodec {
  def encode(r: ResponseFilePart) = {
    val boxed = protobuf.ResponseFilePart(r.data)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseFilePart.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseFilePart(data)) => ResponseFilePart(data)
    }
  }
}
