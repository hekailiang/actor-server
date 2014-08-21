package com.secretapp.backend.protocol.codecs.message.rpc.contact

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scalaz._
import Scalaz._
import scala.util.Success
import com.getsecretapp.{ proto => protobuf }

object ResponsePublicKeysCodec extends Codec[ResponsePublicKeys] with utils.ProtobufCodec {
  def encode(r: ResponsePublicKeys) = {
    val boxed = protobuf.ResponsePublicKeys(r.keys.map(_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponsePublicKeys.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponsePublicKeys(keys)) =>
        ResponsePublicKeys(keys.map(PublicKeyResponse.fromProto(_)))
    }
  }
}
