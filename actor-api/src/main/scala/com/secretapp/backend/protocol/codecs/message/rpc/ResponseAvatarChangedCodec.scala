package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.ResponseAvatarChanged
import com.secretapp.backend.proto
import com.secretapp.backend.models
import com.secretapp.backend.data.message.struct._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object ResponseAvatarChangedCodec extends Codec[ResponseAvatarChanged] with utils.ProtobufCodec {
  def encode(r: ResponseAvatarChanged) = {
    val boxed = protobuf.ResponseAvatarChanged(proto.toProto(r.avatar))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseAvatarChanged.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseAvatarChanged(avatar)) =>
        ResponseAvatarChanged(proto.fromProto[models.Avatar, protobuf.Avatar](avatar))
    }
  }
}
