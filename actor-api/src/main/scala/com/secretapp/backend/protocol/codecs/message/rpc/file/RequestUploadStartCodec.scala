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

object RequestUploadStartCodec extends Codec[RequestStartUpload] with utils.ProtobufCodec {
  def encode(r: RequestStartUpload) = {
    val boxed = protobuf.RequestStartUpload()
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestStartUpload.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestStartUpload()) => RequestStartUpload()
    }
  }
}
