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

object FileUploadedCodec extends Codec[FileUploaded] with utils.ProtobufCodec {
  def encode(r: FileUploaded) = {
    val boxed = protobuf.FileUploaded(r.location.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.FileUploaded.parseFrom(buf.toByteArray)) {
      case Success(protobuf.FileUploaded(location)) => FileUploaded(FileLocation.fromProto(location))
    }
  }
}
