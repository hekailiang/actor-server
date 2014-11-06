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

object ResponseHistoryCodec extends Codec[ResponseHistory] with utils.ProtobufCodec {
  def encode(r: ResponseHistory) = {
    val boxed = protobuf.ResponseHistory(r.history.map(_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseHistory.parseFrom(buf.toByteArray)) {
      case Success(r: protobuf.ResponseHistory) => ResponseHistory(r.history.map(HistoryMessage.fromProto))
    }
  }
}
