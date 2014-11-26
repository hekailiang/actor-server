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

object ResponseCompleteUploadCodec extends Codec[ResponseCompleteUpload] with utils.ProtobufCodec {
  def encode(r: ResponseCompleteUpload) = {
    val boxed = protobuf.ResponseCompleteUpload(proto.toProto(r.location))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseCompleteUpload.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseCompleteUpload(location)) =>
        ResponseCompleteUpload(proto.fromProto[models.FileLocation, protobuf.FileLocation](location))
    }
  }
}
