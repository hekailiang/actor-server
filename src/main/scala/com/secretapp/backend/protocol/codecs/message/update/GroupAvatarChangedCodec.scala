package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.struct.Avatar
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import im.actor.messenger.{ api => protobuf }

import scala.util.Success

object GroupAvatarChangedCodec extends Codec[GroupAvatarChanged] with utils.ProtobufCodec {
  def encode(u: GroupAvatarChanged) = {
    val boxed = protobuf.UpdateGroupAvatarChanged(u.groupId, u.avatar map (_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateGroupAvatarChanged.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateGroupAvatarChanged(groupId, avatar)) =>
        GroupAvatarChanged(groupId, avatar map Avatar.fromProto)
    }
  }
}
