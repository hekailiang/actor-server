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

object ResponseFileUploadStartedCodec extends Codec[ResponseFileUploadStarted] with utils.ProtobufCodec {
  def encode(r: ResponseFileUploadStarted) = {
    val boxed = protobuf.ResponseFileUploadStarted()
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseFileUploadStarted.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseFileUploadStarted()) => ResponseFileUploadStarted()
    }
  }
}
