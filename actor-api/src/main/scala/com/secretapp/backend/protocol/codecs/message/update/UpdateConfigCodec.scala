package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.data.message.struct
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object UpdateConfigCodec extends Codec[UpdateConfig] with utils.ProtobufCodec {
  def encode(u: UpdateConfig) = {
    val boxed = protobuf.UpdateConfig(u.config.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateConfig.parseFrom(buf.toByteArray)) {
      case Success(u) => UpdateConfig(struct.Config.fromProto(u.config))
    }
  }
}
