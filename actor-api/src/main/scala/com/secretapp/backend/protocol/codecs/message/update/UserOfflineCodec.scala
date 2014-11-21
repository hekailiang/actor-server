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

object UserOfflineCodec extends Codec[UserOffline] with utils.ProtobufCodec {
  def encode(u: UserOffline) = {
    val boxed = protobuf.UpdateUserOffline(u.userId)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateUserOffline.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateUserOffline(uid)) => UserOffline(uid)
    }
  }
}
