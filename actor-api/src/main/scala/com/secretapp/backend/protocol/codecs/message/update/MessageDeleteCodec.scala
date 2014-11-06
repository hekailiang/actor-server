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

object MessageDeleteCodec extends Codec[MessageDelete] with utils.ProtobufCodec {
  def encode(u: MessageDelete) = {
    val boxed = protobuf.UpdateMessageDelete(u.peer.toProto, u.randomIds)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateMessageDelete.parseFrom(buf.toByteArray)) {
      case Success(u) => MessageDelete(Peer.fromProto(u.peer), u.rid)
    }
  }
}
