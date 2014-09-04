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
import com.reactive.messenger.{ api => protobuf }

object StateCodec extends Codec[update.State] with utils.ProtobufCodec {
  def encode(s: update.State) = {
    s.state match {
      case Some(state) =>
        uuid.encode(state) match {
          case \/-(bytesState) =>
            val boxed = protobuf.State(s.seq, bytesState)
            encodeToBitVector(boxed)
          case l => l
        }
      case None =>
        val boxed = protobuf.State(s.seq, BitVector.empty)
        encodeToBitVector(boxed)
    }
  }

  def decode(buf: BitVector) = {
    decodeProtobufEither(protobuf.State.parseFrom(buf.toByteArray)) {
      case Success(protobuf.State(seq, state)) =>
        state match {
          case ByteString.EMPTY =>
            update.State(seq, None).right
          case _ =>
            uuid.decodeValue(state) match {
              case \/-(uuidState) =>
                update.State(seq, Some(uuidState)).right
              case l @ -\/(_) =>
                l
            }
        }
    }
  }
}
