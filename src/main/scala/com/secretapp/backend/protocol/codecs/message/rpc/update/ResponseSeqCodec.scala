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
  def encode(s: update.ResponseSeq) = {
    s.state match {
      case Some(state) =>
        uuid.encode(state) match {
          case \/-(bytesState) =>
            val boxed = protobuf.ResponseSeq(s.seq, bytesState)
            encodeToBitVector(boxed)
          case l => l
        }
      case None =>
        val boxed = protobuf.ResponseSeq(s.seq, BitVector.empty)
        encodeToBitVector(boxed)
    }
  }

  def decode(buf: BitVector) = {
    decodeProtobufEither(protobuf.ResponseSeq.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseSeq(seq, state)) =>
        state match {
          case ByteString.EMPTY =>
            update.ResponseSeq(seq, None).right
          case _ =>
            uuid.decodeValue(state) match {
              case \/-(uuidState) =>
                update.ResponseSeq(seq, Some(uuidState)).right
              case l @ -\/(_) =>
                l
            }
        }
    }
  }
}
