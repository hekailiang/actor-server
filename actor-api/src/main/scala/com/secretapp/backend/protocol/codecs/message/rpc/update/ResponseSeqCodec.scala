package com.secretapp.backend.protocol.codecs.message.rpc.update

import com.google.protobuf.ByteString
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.update
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object ResponseSeqCodec extends Codec[update.ResponseSeq] with utils.ProtobufCodec {
  def encode(r: update.ResponseSeq) = {
    val boxed = protobuf.ResponseSeq(r.seq, stateOpt.encodeValid(r.state))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobufEither(protobuf.ResponseSeq.parseFrom(buf.toByteArray)) {
      case Success(r) =>
        stateOpt.decode(r.state) match {
          case \/-((_, state)) => update.ResponseSeq(r.seq, state).right
          case -\/(e) => e.left
        }
    }
  }
}
