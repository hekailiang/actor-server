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

object ResponseFileUploadStartedCodec extends Codec[ResponsePartUploaded] with utils.ProtobufCodec {
  def encode(r: ResponsePartUploaded) = {
    val boxed = protobuf.ResponsePartUploaded()
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponsePartUploaded.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponsePartUploaded()) => ResponsePartUploaded()
    }
  }
}
