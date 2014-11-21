package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.data.message.struct.Peer
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

object ChatDeleteCodec extends Codec[ChatDelete] with utils.ProtobufCodec {
  def encode(u: ChatDelete) = {
    val boxed = protobuf.UpdateChatDelete(u.peer.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateChatDelete.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateChatDelete(peer)) => ChatDelete(Peer.fromProto(peer))
    }
  }
}
