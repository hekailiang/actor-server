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

object RequestTypingCodec extends Codec[RequestTyping] with utils.ProtobufCodec {
  def encode(r: RequestTyping) = {
    val boxed = protobuf.RequestTyping(r.peer.toProto, r.typingType)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestTyping.parseFrom(buf.toByteArray)) {
      case Success(r) => RequestTyping(struct.OutPeer.fromProto(r.peer), r.typingType)
    }
  }
}
