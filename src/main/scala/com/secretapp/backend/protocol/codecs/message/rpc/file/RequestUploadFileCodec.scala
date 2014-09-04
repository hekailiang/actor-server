package com.secretapp.backend.protocol.codecs.message.rpc.file

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scalaz._
import Scalaz._
import scala.util.Success
import com.reactive.messenger.{ api => protobuf }

object RequestUploadFileCodec extends Codec[RequestUploadPart] with utils.ProtobufCodec {
  def encode(r: RequestUploadPart) = {
    val boxed = protobuf.RequestUploadPart(r.config.toProto, r.offset, r.data)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestUploadPart.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestUploadPart(config, offset, data)) =>
        RequestUploadPart(UploadConfig.fromProto(config), offset, data)
    }
  }
}
