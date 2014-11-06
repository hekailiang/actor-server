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

object ResponseMessageSentCodec extends Codec[ResponseMessageSent] with utils.ProtobufCodec {
  def encode(r: ResponseMessageSent) = {
    val boxed = protobuf.ResponseMessageSent(r.seq, stateOpt.encodeValid(r.state), r.date)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobufEither(protobuf.ResponseMessageSent.parseFrom(buf.toByteArray)) {
      case Success(r) =>
        stateOpt.decode(r.state) match {
          case \/-((_, state)) => ResponseMessageSent(r.seq, state, r.date).right
          case -\/(e) => e.left
        }
    }
  }
}
