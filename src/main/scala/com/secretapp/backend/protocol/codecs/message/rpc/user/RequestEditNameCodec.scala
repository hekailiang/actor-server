package com.secretapp.backend.protocol.codecs.message.rpc.user

import com.secretapp.backend.data.message.rpc.user.RequestEditName
import com.secretapp.backend.protocol.codecs.utils
import com.secretapp.backend.protocol.codecs.utils.protobuf.encodeToBitVector
import im.actor.messenger.{ api => protobuf }
import scodec.Codec
import scodec.bits.BitVector

import scala.util.Success
import scalaz.\/

object RequestEditNameCodec extends Codec[RequestEditName] with utils.ProtobufCodec {
  override def encode(r: RequestEditName): \/[String, BitVector] = {
    val boxed = protobuf.RequestEditName(r.name)
    encodeToBitVector(boxed)
  }

  override def decode(buf: BitVector): \/[String, (BitVector, RequestEditName)] = {
    decodeProtobuf(protobuf.RequestEditName.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestEditName(name)) =>
        RequestEditName(name)
    }
  }
}
