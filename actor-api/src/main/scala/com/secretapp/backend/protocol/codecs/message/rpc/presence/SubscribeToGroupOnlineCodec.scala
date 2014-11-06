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

object SubscribeToGroupOnlineCodec extends Codec[SubscribeToGroupOnline] with utils.ProtobufCodec {
  def encode(r: SubscribeToGroupOnline) = {
    val boxed = protobuf.SubscribeToGroupOnline(r.groupIds map (_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.SubscribeToGroupOnline.parseFrom(buf.toByteArray)) {
      case Success(protobuf.SubscribeToGroupOnline(groupIds)) =>
        SubscribeToGroupOnline(groupIds map struct.GroupId.fromProto)
    }
  }
}
