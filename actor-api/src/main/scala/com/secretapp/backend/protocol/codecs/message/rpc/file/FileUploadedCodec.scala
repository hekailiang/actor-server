package com.secretapp.backend.protocol.codecs.message.rpc.file

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.models
import com.secretapp.backend.data.message.struct._
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.proto
import scodec.bits._
import scodec.Codec
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object FileUploadedCodec extends Codec[ResponseUploadCompleted] with utils.ProtobufCodec {
  def encode(r: ResponseUploadCompleted) = {
    val boxed = protobuf.ResponseUploadCompleted(proto.toProto(r.location))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseUploadCompleted.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseUploadCompleted(location)) =>
        ResponseUploadCompleted(proto.fromProto[models.FileLocation, protobuf.FileLocation](location))
    }
  }
}
