package com.secretapp.backend.protocol.codecs.message.rpc.user

import com.secretapp.backend.data.message.rpc.user._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.models
import com.secretapp.backend.data.message.struct._
import scodec.bits._
import scodec.Codec
import scala.util.Success
import im.actor.messenger.{ api => protobuf }
import com.secretapp.backend.proto

object RequestEditAvatarCodec extends Codec[RequestEditAvatar] with utils.ProtobufCodec {
  def encode(r: RequestEditAvatar) = {
    val boxed = protobuf.RequestEditAvatar(proto.toProto(r.fileLocation))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestEditAvatar.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestEditAvatar(fileLocation)) =>
        RequestEditAvatar(proto.fromProto[models.FileLocation, protobuf.FileLocation](fileLocation))
    }
  }
}
