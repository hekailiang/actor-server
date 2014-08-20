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

object RequestUploadStartCodec extends Codec[RequestUploadStart] with utils.ProtobufCodec {
  def encode(r: RequestUploadStart) = {
    val boxed = protobuf.RequestUploadStart()
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestUploadStart.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestUploadStart()) => RequestUploadStart()
    }
  }
}
