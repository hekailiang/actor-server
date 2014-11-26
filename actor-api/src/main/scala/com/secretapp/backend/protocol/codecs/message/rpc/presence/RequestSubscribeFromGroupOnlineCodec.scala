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

object RequestSubscribeFromGroupOnlineCodec extends Codec[RequestSubscribeFromGroupOnline] with utils.ProtobufCodec {
  def encode(r: RequestSubscribeFromGroupOnline) = {
    val boxed = protobuf.RequestSubscribeFromGroupOnline(r.groupIds map (_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestSubscribeFromGroupOnline.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestSubscribeFromGroupOnline(groupIds)) =>
        RequestSubscribeFromGroupOnline(groupIds map struct.GroupOutPeer.fromProto)
    }
  }
}
