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

object UserOnlineUpdateCodec extends Codec[UserOnlineUpdate] with utils.ProtobufCodec {
  def encode(u: UserOnlineUpdate) = {
    val boxed = protobuf.UserOnlineUpdate(u.uid)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UserOnlineUpdate.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UserOnlineUpdate(uid)) => UserOnlineUpdate(uid)
    }
  }
}
