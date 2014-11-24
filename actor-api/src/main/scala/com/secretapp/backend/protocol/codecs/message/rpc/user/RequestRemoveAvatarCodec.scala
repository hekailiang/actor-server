package com.secretapp.backend.protocol.codecs.message.rpc.user

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.user.RequestRemoveAvatar
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object RequestRemoveAvatarCodec extends Codec[RequestRemoveAvatar] with utils.ProtobufCodec {
  def encode(r: RequestRemoveAvatar) = {
    val boxed = protobuf.RequestRemoveAvatar()
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestRemoveAvatar.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestRemoveAvatar()) => RequestRemoveAvatar()
    }
  }
}
