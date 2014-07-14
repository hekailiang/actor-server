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
import scala.util.Success
import com.getsecretapp.{ proto => protobuf }

object DifferenceCodec extends Codec[Difference] with utils.ProtobufCodec {
  def encode(d : Difference) = {
    val boxed = protobuf.Difference(d.seq, d.state, d.users.map(_.toProto), d.updates.map(_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf : BitVector) = {
    decodeProtobuf(protobuf.Difference.parseFrom(buf.toByteArray)) {
      case Success(protobuf.Difference(seq, state, users, updates)) =>
        Difference(seq, state, users.map(struct.User.fromProto(_)), updates.map(CommonUpdate.fromProto(_)))
    }
  }
}
