package com.secretapp.backend.protocol.codecs.message.rpc.contact

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object RequestGetPublicKeysCodec extends Codec[RequestGetPublicKeys] with utils.ProtobufCodec {
  def encode(r: RequestGetPublicKeys) = {
    val boxed = protobuf.RequestGetPublicKeys(r.keys.map(_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobufEither(protobuf.RequestGetPublicKeys.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestGetPublicKeys(keys)) =>
        if (keys.length > 100) {
          "TOO_MANY".left
        } else {
          RequestGetPublicKeys(keys.map(PublicKeyRequest.fromProto(_))).right
        }
    }
  }
}
