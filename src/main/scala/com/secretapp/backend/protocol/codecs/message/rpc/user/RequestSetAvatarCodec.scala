package com.secretapp.backend.protocol.codecs.message.rpc.user

import com.secretapp.backend.data.message.rpc.file.FileLocation
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.user._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import com.reactive.messenger.{ api => protobuf }

object RequestSetAvatarCodec extends Codec[RequestSetAvatar] with utils.ProtobufCodec {
  def encode(r: RequestSetAvatar) = {
    val boxed = protobuf.RequestSetAvatar(r.fileLocation.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestSetAvatar.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestSetAvatar(fileLocation)) =>
        RequestSetAvatar(FileLocation.fromProto(fileLocation))
    }
  }
}
