package com.secretapp.backend.protocol.codecs.message.rpc.messaging

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import scodec.bits.BitVector
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object RequestCreateGroupCodec extends Codec[RequestCreateGroup] with utils.ProtobufCodec {
  def encode(r: RequestCreateGroup) = {
    val boxed = protobuf.RequestCreateGroup(r.randomId, r.title, r.users.map(_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestCreateGroup.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestCreateGroup(randomId, title, users)) =>
        RequestCreateGroup(randomId, title, users.map(struct.UserOutPeer.fromProto))
    }
  }
}
