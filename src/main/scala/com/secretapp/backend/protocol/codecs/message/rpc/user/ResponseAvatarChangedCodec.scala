package com.secretapp.backend.protocol.codecs.message.rpc.user

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.user._
import com.secretapp.backend.data.message.struct.Avatar
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object ResponseAvatarChangedCodec extends Codec[ResponseAvatarChanged] with utils.ProtobufCodec {
  def encode(r: ResponseAvatarChanged) = {
    val boxed = protobuf.ResponseAvatarChanged(r.avatar.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseAvatarChanged.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseAvatarChanged(avatar)) =>
        ResponseAvatarChanged(Avatar.fromProto(avatar))
    }
  }
}
