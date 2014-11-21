package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.ResponseAvatarChanged
import com.secretapp.backend.proto
import com.secretapp.backend.data.message.struct._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{ api => protobuf }
import scalaz._
import scalaz.Scalaz._
import scala.util.Success
import scodec.bits._
import scodec.Codec

object ResponseAvatarChangedCodec extends Codec[ResponseAvatarChanged] with utils.ProtobufCodec {
  def encode(r: ResponseAvatarChanged) = {
    val boxed = protobuf.ResponseAvatarChanged(proto.toProto(r.avatar), r.seq, stateOpt.encodeValid(r.state))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobufEither(protobuf.ResponseAvatarChanged.parseFrom(buf.toByteArray)) {
      case Success(r) =>
        stateOpt.decode(r.state) match {
          case \/-((_, state)) => ResponseAvatarChanged(proto.fromProto(r.avatar), r.seq, state).right
          case -\/(e) => e.left
        }
    }
  }
}
