package com.secretapp.backend.protocol.codecs.message.rpc.file

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.struct.FileLocation
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object FileUploadedCodec extends Codec[ResponseUploadCompleted] with utils.ProtobufCodec {
  def encode(r: ResponseUploadCompleted) = {
    val boxed = protobuf.ResponseUploadCompleted(r.location.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseUploadCompleted.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseUploadCompleted(location)) => ResponseUploadCompleted(FileLocation.fromProto(location))
    }
  }
}
