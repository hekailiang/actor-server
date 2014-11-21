package com.secretapp.backend.protocol.codecs.message.rpc.history

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.history._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import scodec.bits.BitVector
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object RequestLoadHistoryCodec extends Codec[RequestLoadHistory] with utils.ProtobufCodec {
  def encode(r: RequestLoadHistory) = {
    val boxed = protobuf.RequestLoadHistory(r.outPeer.toProto, r.startDate, r.limit)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestLoadHistory.parseFrom(buf.toByteArray)) {
      case Success(r: protobuf.RequestLoadHistory) =>
        RequestLoadHistory(struct.OutPeer.fromProto(r.peer), r.startDate, r.limit)
    }
  }
}
