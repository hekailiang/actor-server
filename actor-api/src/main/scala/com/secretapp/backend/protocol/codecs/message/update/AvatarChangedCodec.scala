package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.data.message.update._
import com.secretapp.backend.models
import com.secretapp.backend.data.message.struct._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.proto
import scodec.bits._
import scodec.Codec
import im.actor.messenger.{ api => protobuf }

import scala.util.Success

object AvatarChangedCodec extends Codec[AvatarChanged] with utils.ProtobufCodec {
  def encode(n: AvatarChanged) = {
    val boxed = protobuf.UpdateAvatarChanged(n.userId, n.avatar map proto.toProto[models.Avatar, protobuf.Avatar])
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateAvatarChanged.parseFrom(buf.toByteArray)) {
      case Success(r) => AvatarChanged(r.uid, r.avatar.map(proto.fromProto[models.Avatar, protobuf.Avatar]))
    }
  }
}
