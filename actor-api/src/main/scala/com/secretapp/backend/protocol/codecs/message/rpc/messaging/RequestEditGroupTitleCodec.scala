package com.secretapp.backend.protocol.codecs.message.rpc.user

import com.secretapp.backend.data.message.rpc.messaging.RequestEditGroupTitle
import com.secretapp.backend.protocol.codecs.utils
import com.secretapp.backend.protocol.codecs.utils.protobuf.encodeToBitVector
import im.actor.messenger.{ api => protobuf }
import scodec.Codec
import scodec.bits.BitVector
import scala.util.Success
import scalaz.\/

object RequestEditGroupTitleCodec extends Codec[RequestEditGroupTitle] with utils.ProtobufCodec {
  override def encode(r: RequestEditGroupTitle): \/[String, BitVector] = {
    val boxed = protobuf.RequestEditGroupTitle(r.groupId, r.accessHash, r.title)
    encodeToBitVector(boxed)
  }

  override def decode(buf: BitVector): \/[String, (BitVector, RequestEditGroupTitle)] = {
    decodeProtobuf(protobuf.RequestEditGroupTitle.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestEditGroupTitle(groupId, accessHash, title)) =>
        RequestEditGroupTitle(groupId, accessHash, title)
    }
  }
}
