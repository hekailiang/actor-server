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

object EncryptedReadCodec extends Codec[EncryptedRead] with utils.ProtobufCodec {
  def encode(m: EncryptedRead) = {
    val boxed = protobuf.UpdateEncryptedRead(m.peer.toProto, m.randomId, m.readDate)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateEncryptedRead.parseFrom(buf.toByteArray)) {
      case Success(e) => EncryptedRead(Peer.fromProto(e.peer), e.rid, e.readDate)
    }
  }
}
