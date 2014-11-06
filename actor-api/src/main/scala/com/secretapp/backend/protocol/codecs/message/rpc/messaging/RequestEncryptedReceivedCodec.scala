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

object RequestEncryptedReceivedCodec extends Codec[RequestEncryptedReceived] with utils.ProtobufCodec {
  override def encode(r: RequestEncryptedReceived) = {
    val boxed = protobuf.RequestEncryptedReceived(r.peer.toProto, r.randomId)
    encodeToBitVector(boxed)
  }

  override def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestEncryptedReceived.parseFrom(buf.toByteArray)) {
      case Success(r) => RequestEncryptedReceived(struct.OutPeer.fromProto(r.peer), r.rid)
    }
  }
}
