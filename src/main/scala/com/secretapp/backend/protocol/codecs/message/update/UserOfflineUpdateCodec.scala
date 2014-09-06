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

object UserOfflineUpdateCodec extends Codec[UserOfflineUpdate] with utils.ProtobufCodec {
  def encode(u: UserOfflineUpdate) = {
    val boxed = protobuf.UserOfflineUpdate(u.uid)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UserOfflineUpdate.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UserOfflineUpdate(uid)) => UserOfflineUpdate(uid)
    }
  }
}
