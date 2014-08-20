package com.secretapp.backend.protocol.codecs.message.rpc.file

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scalaz._
import Scalaz._
import scala.util.Success
import com.secretapp.{ proto => protobuf }

object ResponseUploadStartCodec extends Codec[ResponseUploadStart] with utils.ProtobufCodec {
  def encode(r: ResponseUploadStart) = {
    val boxed = protobuf.ResponseUploadStart(r.config.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseUploadStart.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseUploadStart(config)) => ResponseUploadStart(UploadConfig.fromProto(config))
    }
  }
}
