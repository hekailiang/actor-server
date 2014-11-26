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

object ResponseGetFileCodec extends Codec[ResponseGetFile] with utils.ProtobufCodec {
  def encode(r: ResponseGetFile) = {
    val boxed = protobuf.ResponseGetFile(r.data)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseGetFile.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseGetFile(data)) => ResponseGetFile(data)
    }
  }
}
