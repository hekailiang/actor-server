package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import com.reactive.messenger.{ api => protobuf }

object UserLastSeenUpdateCodec extends Codec[UserLastSeenUpdate] with utils.ProtobufCodec {
  def encode(u: UserLastSeenUpdate) = {
    val boxed = protobuf.UserLastSeenUpdate(u.uid, u.time)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UserLastSeenUpdate.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UserLastSeenUpdate(uid, time)) => UserLastSeenUpdate(uid, time)
    }
  }
}
