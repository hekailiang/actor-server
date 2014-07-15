package com.secretapp.backend.protocol.codecs.message.rpc.update

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
import com.getsecretapp.{ proto => protobuf }

object DifferenceCodec extends Codec[Difference] with utils.ProtobufCodec {
  def encode(d: Difference) = {
    d.updates.map(_.toProto).toList.sequenceU match {
      case \/-(updates) =>
        val boxed = protobuf.Difference(d.seq, d.state, d.users.map(_.toProto), updates, d.needMore)
        encodeToBitVector(boxed)
      case l@(-\/(_)) => l
    }
  }

  def decode(buf: BitVector) = {
    Try(protobuf.Difference.parseFrom(buf.toByteArray)) match {
      case Success(protobuf.Difference(seq, state, users, updates, needMore)) =>
        updates.map(CommonUpdate.fromProto(_)).toList.sequenceU match {
          case \/-(updates) =>
            val unboxed = Difference(seq, state, users.map(struct.User.fromProto(_)), updates, needMore)
            (BitVector.empty, unboxed).right
          case l@(-\/(_)) => l
        }
      case Failure(e) => s"parse error: ${e.getMessage}".left
    }
  }
}
