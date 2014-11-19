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

object MessageReadByMeCodec extends Codec[MessageReadByMe] with utils.ProtobufCodec {
  def encode(u: MessageReadByMe) = {
    val boxed = protobuf.UpdateMessageReadByMe(u.peer.toProto, u.date)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateMessageReadByMe.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateMessageReadByMe(peer, date)) =>
        MessageReadByMe(Peer.fromProto(peer), date)
    }
  }
}
