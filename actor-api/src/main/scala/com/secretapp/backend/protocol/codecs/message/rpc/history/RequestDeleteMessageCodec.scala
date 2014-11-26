package com.secretapp.backend.protocol.codecs.message.rpc.history

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.history._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import scodec.bits.BitVector
import scodec.Codec
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object RequestDeleteMessageCodec extends Codec[RequestDeleteMessage] with utils.ProtobufCodec {
  def encode(r: RequestDeleteMessage) = {
    val boxed = protobuf.RequestDeleteMessage(r.outPeer.toProto, r.randomIds)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestDeleteMessage.parseFrom(buf.toByteArray)) {
      case Success(r) => RequestDeleteMessage(struct.OutPeer.fromProto(r.peer), r.rids)
    }
  }
}
