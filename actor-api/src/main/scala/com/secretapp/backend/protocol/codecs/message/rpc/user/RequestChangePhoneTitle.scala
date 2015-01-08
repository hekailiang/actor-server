package com.secretapp.backend.protocol.codecs.message.rpc.user

import com.secretapp.backend.data.message.rpc.user.RequestChangePhoneTitle
import com.secretapp.backend.protocol.codecs.utils
import com.secretapp.backend.protocol.codecs.utils.protobuf.encodeToBitVector
import im.actor.messenger.{ api => protobuf }
import scodec.Codec
import scodec.bits.BitVector

import scala.util.Success
import scalaz.\/

object RequestChangePhoneTitleCodec extends Codec[RequestChangePhoneTitle] with utils.ProtobufCodec {
  override def encode(r: RequestChangePhoneTitle): \/[String, BitVector] = {
    val boxed = protobuf.RequestChangePhoneTitle(r.phoneId, r.title)
    encodeToBitVector(boxed)
  }

  override def decode(buf: BitVector): \/[String, (BitVector, RequestChangePhoneTitle)] = {
    decodeProtobuf(protobuf.RequestChangePhoneTitle.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestChangePhoneTitle(id, title)) =>
        RequestChangePhoneTitle(id, title)
    }
  }
}
