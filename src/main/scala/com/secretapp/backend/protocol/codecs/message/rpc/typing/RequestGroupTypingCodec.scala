package com.secretapp.backend.protocol.codecs.message.rpc.typing

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.typing._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import scodec.bits.BitVector
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object RequestGroupTypingCodec extends Codec[RequestGroupTyping] with utils.ProtobufCodec {
  def encode(r: RequestGroupTyping) = {
    val boxed = protobuf.RequestGroupTyping(r.groupId, r.accessHash, r.typingType)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestGroupTyping.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestGroupTyping(groupId, accessHash, typingType)) =>
        RequestGroupTyping(groupId, accessHash, typingType)
    }
  }
}
