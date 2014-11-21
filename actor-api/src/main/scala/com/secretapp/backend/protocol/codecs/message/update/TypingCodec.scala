package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.data.message.struct
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object TypingCodec extends Codec[Typing] with utils.ProtobufCodec {
  def encode(u: Typing) = {
    val boxed = protobuf.UpdateTyping(u.peer.toProto, u.userId, u.typingType)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateTyping.parseFrom(buf.toByteArray)) {
      case Success(u) => Typing(struct.Peer.fromProto(u.peer), u.uid, u.typingType)
    }
  }
}
