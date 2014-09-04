package com.secretapp.backend.protocol.codecs.message.rpc.update

import com.google.protobuf.ByteString
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.rpc.update._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scala.util.Success
import scalaz._
import scalaz.Scalaz._
import com.reactive.messenger.{ api => protobuf }

object RequestGetDifferenceCodec extends Codec[RequestGetDifference] with utils.ProtobufCodec {
  def encode(r: RequestGetDifference) = {
    r.state map (uuid.encode(_)) getOrElse(BitVector.empty.right) match {
      case \/-(encodedUuid) =>
        val boxed = protobuf.RequestGetDifference(r.seq, encodedUuid)
        encodeToBitVector(boxed)
      case l @ -\/(_) => l
    }
  }

  def decode(buf: BitVector) = {
    decodeProtobufEither(protobuf.RequestGetDifference.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestGetDifference(seq, state)) =>
        val decodedState = if (state == ByteString.EMPTY) {
          None.right
        } else {
          uuid.decodeValue(state) map (Some(_))
        }

        decodedState match {
          case \/-(uuidState) =>
            RequestGetDifference(seq, uuidState).right
          case l @ -\/(_) => l
        }
    }
  }
}
