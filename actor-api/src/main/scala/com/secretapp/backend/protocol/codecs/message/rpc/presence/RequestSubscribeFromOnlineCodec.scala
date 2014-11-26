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

object RequestSubscribeFromOnlineCodec extends Codec[RequestSubscribeFromOnline] with utils.ProtobufCodec {
  def encode(r: RequestSubscribeFromOnline) = {
    val boxed = protobuf.RequestSubscribeFromOnline(r.users map (_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestSubscribeFromOnline.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestSubscribeFromOnline(userIds)) =>
        RequestSubscribeFromOnline(userIds map (struct.UserOutPeer.fromProto(_)))
    }
  }
}
