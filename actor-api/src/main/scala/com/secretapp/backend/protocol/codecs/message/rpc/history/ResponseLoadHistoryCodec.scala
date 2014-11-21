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

object ResponseLoadHistoryCodec extends Codec[ResponseLoadHistory] with utils.ProtobufCodec {
  def encode(r: ResponseLoadHistory) = {
    val boxed = protobuf.ResponseLoadHistory(r.history.map(_.toProto), r.users.map(_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseLoadHistory.parseFrom(buf.toByteArray)) {
      case Success(r: protobuf.ResponseLoadHistory) =>
        ResponseLoadHistory(r.history.map(HistoryMessage.fromProto), r.users.map(struct.User.fromProto))
    }
  }
}
