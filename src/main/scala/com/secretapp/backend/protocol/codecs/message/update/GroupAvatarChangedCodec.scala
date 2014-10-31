package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.data.message.update._
import com.secretapp.backend.models
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.proto
import com.secretapp.backend.data.message.struct._
import scodec.bits._
import scodec.Codec
import im.actor.messenger.{ api => protobuf }

import scala.util.Success

object GroupAvatarChangedCodec extends Codec[GroupAvatarChanged] with utils.ProtobufCodec {
  def encode(u: GroupAvatarChanged) = {
    val boxed = protobuf.UpdateGroupAvatarChanged(u.groupId, u.avatar map proto.toProto[models.Avatar, protobuf.Avatar])
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateGroupAvatarChanged.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateGroupAvatarChanged(groupId, avatar)) =>
        GroupAvatarChanged(groupId, avatar map proto.fromProto[models.Avatar, protobuf.Avatar])
    }
  }
}
