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

object ResponseGetPublicKeysCodec extends Codec[ResponseGetPublicKeys] with utils.ProtobufCodec {
  def encode(r: ResponseGetPublicKeys) = {
    val boxed = protobuf.ResponseGetPublicKeys(r.keys.map(_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseGetPublicKeys.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseGetPublicKeys(keys)) =>
        ResponseGetPublicKeys(keys.map(PublicKeyResponse.fromProto(_)))
    }
  }
}
