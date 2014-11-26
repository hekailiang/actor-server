package com.secretapp.backend.protocol.codecs.message.rpc.update

import com.google.protobuf.ByteString
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.struct
import scodec.bits.BitVector
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.{ Try, Success, Failure }
import im.actor.messenger.{ api => protobuf }

object ResponseGetDifferenceCodec extends Codec[ResponseGetDifference] with utils.ProtobufCodec {
  def encode(d: ResponseGetDifference) = {
    d.updates.map(_.toProto).toList.sequenceU match {
      case \/-(updates) =>
        val boxed = protobuf.ResponseGetDifference(d.seq, stateOpt.encodeValid(d.state), d.users.map(_.toProto),
          d.groups.map(_.toProto), updates, d.needMore)
        encodeToBitVector(boxed)
      case l@(-\/(_)) => l
    }
  }

  def decode(buf: BitVector) = {
    decodeProtobufEither(protobuf.ResponseGetDifference.parseFrom(buf.toByteArray)) {
      case Success(r) =>
        r.updates.map(DifferenceUpdate.fromProto).toList.sequenceU match {
          case \/-(updates) =>
            stateOpt.decodeValue(r.state) match {
              case \/-(state) =>
                ResponseGetDifference(r.seq, state, r.users.map(struct.User.fromProto),
                  r.groups.map(struct.Group.fromProto), updates, r.needMore).right
              case l @ (-\/(_)) => l
            }
          case l@(-\/(_)) => l
        }
    }
  }
}
