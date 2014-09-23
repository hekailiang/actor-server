package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.data.message.struct.Avatar
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import com.reactive.messenger.{ api => protobuf }

import scala.util.Success

object AvatarChangedCodec extends Codec[AvatarChanged] with utils.ProtobufCodec {
  def encode(n: AvatarChanged) = {
    val boxed = protobuf.UpdateAvatarChanged(n.uid, n.avatar.map(_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateAvatarChanged.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateAvatarChanged(uid, av)) => AvatarChanged(uid, av.map(Avatar.fromProto))
    }
  }
}
