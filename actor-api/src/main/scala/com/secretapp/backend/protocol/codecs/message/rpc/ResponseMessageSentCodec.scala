package com.secretapp.backend.protocol.codecs.message.rpc.messaging

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import scodec.bits.BitVector
import scodec.Codec
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object ResponseSeqDateCodec extends Codec[ResponseSeqDate] with utils.ProtobufCodec {
  def encode(r: ResponseSeqDate) = {
    val boxed = protobuf.ResponseSeqDate(r.seq, stateOpt.encodeValid(r.state), r.date)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobufEither(protobuf.ResponseSeqDate.parseFrom(buf.toByteArray)) {
      case Success(r) =>
        stateOpt.decode(r.state) match {
          case \/-((_, state)) => ResponseSeqDate(r.seq, state, r.date).right
          case -\/(e) => e.left
        }
    }
  }
}
