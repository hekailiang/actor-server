package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import im.actor.messenger.{ api => protobuf }

import scala.util.Success

object NameChangedCodec extends Codec[NameChanged] with utils.ProtobufCodec {
  def encode(u: NameChanged) = {
    val boxed = protobuf.UpdateUserNameChanged(u.userId, u.name)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateUserNameChanged.parseFrom(buf.toByteArray)) {
      case Success(r) => NameChanged(r.uid, r.name)
    }
  }
}
