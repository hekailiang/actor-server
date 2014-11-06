package com.secretapp.backend.protocol.codecs.message.rpc.presence

import com.secretapp.backend.data.message.struct
import scodec.bits._
import scodec.Codec
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.protocol.codecs._
import scodec.codecs._
import im.actor.messenger.{ api => protobuf }
import scalaz._
import scalaz.Scalaz._
import scala.util.Success

object UnsubscribeFromOnlineCodec extends Codec[UnsubscribeFromOnline] with utils.ProtobufCodec {
  def encode(r: UnsubscribeFromOnline) = {
    val boxed = protobuf.UnsubscribeFromOnline(r.users map (_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UnsubscribeFromOnline.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UnsubscribeFromOnline(userIds)) =>
        UnsubscribeFromOnline(userIds map (struct.UserId.fromProto(_)))
    }
  }
}
