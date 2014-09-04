package com.secretapp.backend.protocol.codecs.message.rpc.contact

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scalaz._
import Scalaz._
import scala.util.Success
import com.reactive.messenger.{ api => protobuf }

object RequestPublicKeysCodec extends Codec[RequestPublicKeys] with utils.ProtobufCodec {
  def encode(r: RequestPublicKeys) = {
    val boxed = protobuf.RequestPublicKeys(r.keys.map(_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobufEither(protobuf.RequestPublicKeys.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestPublicKeys(keys)) =>
        if (keys.length > 100) {
          "TOO_MANY".left
        } else {
          RequestPublicKeys(keys.map(PublicKeyRequest.fromProto(_))).right
        }
    }
  }
}
