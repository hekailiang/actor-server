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

object ResponseUploadStartedCodec extends Codec[ResponseUploadStarted] with utils.ProtobufCodec {
  def encode(r: ResponseUploadStarted) = {
    val boxed = protobuf.ResponseUploadStarted(r.config.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseUploadStarted.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseUploadStarted(config)) => ResponseUploadStarted(UploadConfig.fromProto(config))
    }
  }
}
