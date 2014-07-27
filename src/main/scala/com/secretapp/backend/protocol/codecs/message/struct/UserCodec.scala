package com.secretapp.backend.protocol.codecs.message.struct

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.types
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import com.getsecretapp.{ proto => protobuf }

object UserCodec extends Codec[struct.User] with utils.ProtobufCodec {
  def encode(u: struct.User) = {
    val boxed = protobuf.User(u.uid, u.accessHash, u.firstName, u.lastName, u.sex.flatMap(_.toProto.some),
      u.keyHashes.toIndexedSeq)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.User.parseFrom(buf.toByteArray)) {
      case Success(protobuf.User(id, accessHash, firstName, lastName, sex, keyHashes)) =>
        struct.User(id, accessHash, firstName, lastName, sex.flatMap(types.Sex.fromProto(_).some), keyHashes.toSet)
    }
  }
}
