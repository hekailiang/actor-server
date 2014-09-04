package com.secretapp.backend.protocol.codecs.message.rpc.presence

import com.secretapp.backend.data.message.struct
import scodec.bits._
import scodec.Codec
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.protocol.codecs._
import scodec.codecs._
import com.reactive.messenger.{ api => protobuf }
import scalaz._
import scalaz.Scalaz._
import scala.util.Success

object UnsubscribeForOnlineCodec extends Codec[UnsubscribeForOnline] with utils.ProtobufCodec {
  def encode(r: UnsubscribeForOnline) = {
    val boxed = protobuf.UnsubscribeForOnline(r.users map (_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UnsubscribeForOnline.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UnsubscribeForOnline(userIds)) =>
        UnsubscribeForOnline(userIds map (struct.UserId.fromProto(_)))
    }
  }
}
