package com.secretapp.backend.protocol.codecs.message.rpc.messaging

import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.protocol.codecs.utils
import com.secretapp.backend.protocol.codecs.utils.protobuf.encodeToBitVector
import im.actor.messenger.{ api => protobuf }
import scodec.Codec
import scodec.bits.BitVector
import scala.util.Success
import scalaz.\/

object RequestEncryptedReadCodec extends Codec[RequestEncryptedRead] with utils.ProtobufCodec {
  override def encode(r: RequestEncryptedRead): \/[String, BitVector] = {
    val boxed = protobuf.RequestEncryptedRead(r.peer.toProto, r.randomId)
    encodeToBitVector(boxed)
  }

  override def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestEncryptedRead.parseFrom(buf.toByteArray)) {
      case Success(r) => RequestEncryptedRead(struct.OutPeer.fromProto(r.peer), r.rid)
    }
  }
}
