package com.secretapp.backend.protocol.codecs.message.struct

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.types
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.{ Try, Success, Failure }
import com.getsecretapp.{ proto => protobuf }

object UserCodec extends Codec[struct.User] {
  def encode(u : struct.User) = {
    val boxed = protobuf.User(u.id, u.accessHash, u.firstName, u.lastName, u.sex.flatMap(_.toProto.some), u.keyHashes)
    encodeToBitVector(boxed)
  }

  def decode(buf : BitVector) = {
    Try(protobuf.User.parseFrom(buf.toByteArray)) match {
      case Success(protobuf.User(id, accessHash, firstName, lastName, sex, keyHashes)) =>
        ( BitVector.empty,
          struct.User(id, accessHash, firstName, lastName, sex.flatMap(types.Sex.fromProto(_).some), keyHashes) ).right
      case Failure(e) => s"parse error: ${e.getMessage}".left
    }
  }
}
