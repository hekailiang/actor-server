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

object RequestCompleteUploadCodec extends Codec[RequestCompleteUpload] with utils.ProtobufCodec {
  def encode(r: RequestCompleteUpload) = {
    val boxed = protobuf.RequestCompleteUpload(r.config.toProto, r.blockCount, r.crc32)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestCompleteUpload.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestCompleteUpload(config, blockCount, crc32)) =>
        RequestCompleteUpload(UploadConfig.fromProto(config), blockCount, crc32)
    }
  }
}
