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

object ResponseStartUploadCodec extends Codec[ResponseStartUpload] with utils.ProtobufCodec {
  def encode(r: ResponseStartUpload) = {
    val boxed = protobuf.ResponseStartUpload(r.config.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseStartUpload.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseStartUpload(config)) => ResponseStartUpload(UploadConfig.fromProto(config))
    }
  }
}
