package com.secretapp.backend.protocol.codecs.message.rpc.user

import com.secretapp.backend.data.message.struct.FileLocation
import com.secretapp.backend.data.message.rpc.user._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object RequestEditAvatarCodec extends Codec[RequestEditAvatar] with utils.ProtobufCodec {
  def encode(r: RequestEditAvatar) = {
    val boxed = protobuf.RequestEditAvatar(r.fileLocation.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestEditAvatar.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestEditAvatar(fileLocation)) =>
        RequestEditAvatar(FileLocation.fromProto(fileLocation))
    }
  }
}
