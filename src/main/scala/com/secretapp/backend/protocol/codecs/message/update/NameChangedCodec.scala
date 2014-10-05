package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.data.message.struct.Avatar
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import im.actor.messenger.{ api => protobuf }

import scala.util.Success

object NameChangedCodec extends Codec[NameChanged] with utils.ProtobufCodec {
  def encode(u: NameChanged) = {
    val boxed = protobuf.UpdateUserNameChanged(u.uid, u.name)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateUserNameChanged.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateUserNameChanged(uid, name)) => NameChanged(uid, name)
    }
  }
}
