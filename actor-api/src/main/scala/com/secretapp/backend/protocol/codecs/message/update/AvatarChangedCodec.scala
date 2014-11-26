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

object UserAvatarChangedCodec extends Codec[UserAvatarChanged] with utils.ProtobufCodec {
  def encode(n: UserAvatarChanged) = {
    val boxed = protobuf.UpdateUserAvatarChanged(n.userId, n.avatar map proto.toProto[models.Avatar, protobuf.Avatar])
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateUserAvatarChanged.parseFrom(buf.toByteArray)) {
      case Success(r) => UserAvatarChanged(r.uid, r.avatar.map(proto.fromProto[models.Avatar, protobuf.Avatar]))
    }
  }
}
