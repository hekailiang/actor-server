package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.data.message.struct.{User, Avatar}
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import com.reactive.messenger.{ api => protobuf }

import scala.util.Success

object UserChangedCodec extends Codec[UserChanged] with utils.ProtobufCodec {
  def encode(n: UserChanged) = {
    val boxed = protobuf.UpdateUserChanged(n.user.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateUserChanged.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateUserChanged(user)) => UserChanged(User.fromProto(user))
    }
  }
}
