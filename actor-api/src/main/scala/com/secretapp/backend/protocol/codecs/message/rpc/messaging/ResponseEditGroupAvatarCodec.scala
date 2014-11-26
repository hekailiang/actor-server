package com.secretapp.backend.protocol.codecs.message.rpc.messaging

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.messaging.ResponseEditGroupAvatar
import com.secretapp.backend.proto
import com.secretapp.backend.data.message.struct._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{ api => protobuf }
import scalaz._
import scalaz.Scalaz._
import scala.util.Success
import scodec.bits._
import scodec.Codec

object ResponseEditGroupAvatarCodec extends Codec[ResponseEditGroupAvatar] with utils.ProtobufCodec {
  def encode(r: ResponseEditGroupAvatar) = {
    val boxed = protobuf.ResponseEditGroupAvatar(proto.toProto(r.avatar), r.seq, stateOpt.encodeValid(r.state), r.date)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobufEither(protobuf.ResponseEditGroupAvatar.parseFrom(buf.toByteArray)) {
      case Success(r) =>
        stateOpt.decode(r.state) match {
          case \/-((_, state)) => ResponseEditGroupAvatar(proto.fromProto(r.avatar), r.seq, state, r.date).right
          case -\/(e) => e.left
        }
    }
  }
}
