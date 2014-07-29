package com.secretapp.backend.protocol.codecs.message.rpc.file

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scalaz._
import Scalaz._
import scala.util.Success
import com.getsecretapp.{ proto => protobuf }
import com.google.protobuf.{ ByteString => ProtoByteString }

object RequestUploadFileCodec extends Codec[RequestUploadFile] with utils.ProtobufCodec {
  def encode(r: RequestUploadFile) = {
    val boxed = protobuf.RequestUploadFile(r.config.toProto, r.offset, r.data)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestUploadFile.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestUploadFile(config, offset, data)) =>
        RequestUploadFile(UploadConfig.fromProto(config), offset, data)
    }
  }
}
