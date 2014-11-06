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
import im.actor.messenger.{ api => protobuf }

object UserLastSeenCodec extends Codec[UserLastSeen] with utils.ProtobufCodec {
  def encode(u: UserLastSeen) = {
    val boxed = protobuf.UpdateUserLastSeen(u.uid, u.time)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateUserLastSeen.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateUserLastSeen(uid, time)) => UserLastSeen(uid, time)
    }
  }
}
