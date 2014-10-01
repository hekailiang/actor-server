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

object RequestGetFileCodec extends Codec[RequestGetFile] with utils.ProtobufCodec {
  def encode(r: RequestGetFile) = {
    val boxed = protobuf.RequestGetFile(r.fileLocation.toProto, r.offset, r.limit)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestGetFile.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestGetFile(fileLocation, offset, limit)) =>
        RequestGetFile(FileLocation.fromProto(fileLocation), offset, limit)
    }
  }
}
