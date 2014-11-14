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
import im.actor.messenger.{ api => protobuf }

object RequestGetDifferenceCodec extends Codec[RequestGetDifference] with utils.ProtobufCodec {
  def encode(r: RequestGetDifference) = {
    val boxed = protobuf.RequestGetDifference(r.seq, stateOpt.encodeValid(r.state))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobufEither(protobuf.RequestGetDifference.parseFrom(buf.toByteArray)) {
      case Success(r) =>
        stateOpt.decodeValue(r.state) match {
          case \/-(state) => RequestGetDifference(r.seq, state).right
          case l @ -\/(_) => l
        }
    }
  }
}
