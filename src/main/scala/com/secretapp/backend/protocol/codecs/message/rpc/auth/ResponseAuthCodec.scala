package com.secretapp.backend.protocol.codecs.message.rpc.auth

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import com.secretapp.{ proto => protobuf }

object ResponseAuthCodec extends Codec[ResponseAuth] with utils.ProtobufCodec {
  def encode(r: ResponseAuth) = {
    val boxed = protobuf.ResponseAuth(r.publicKeyHash, r.user.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseAuth.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseAuth(publicKeyHash, user)) =>
        ResponseAuth(publicKeyHash, struct.User.fromProto(user))
    }
  }
}
