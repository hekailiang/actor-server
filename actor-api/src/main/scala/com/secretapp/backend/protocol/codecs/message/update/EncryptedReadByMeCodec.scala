package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.data.message.struct.Peer
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object EncryptedReadByMeCodec extends Codec[EncryptedReadByMe] with utils.ProtobufCodec {
  def encode(m: EncryptedReadByMe) = {
    val boxed = protobuf.UpdateEncryptedReadByMe(m.peer.toProto, m.randomId)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateEncryptedReadByMe.parseFrom(buf.toByteArray)) {
      case Success(e) => EncryptedReadByMe(Peer.fromProto(e.peer), e.rid)
    }
  }
}
