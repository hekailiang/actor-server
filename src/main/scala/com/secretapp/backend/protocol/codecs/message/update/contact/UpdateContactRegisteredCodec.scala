package com.secretapp.backend.protocol.codecs.message.update.contact

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.update.contact._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object UpdateContactRegisteredCodec extends Codec[UpdateContactRegistered] with utils.ProtobufCodec {
  def encode(u: UpdateContactRegistered) = encodeToBitVector(u.toProto)

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateContactRegistered.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateContactRegistered(uid)) => UpdateContactRegistered(uid)
    }
  }
}
