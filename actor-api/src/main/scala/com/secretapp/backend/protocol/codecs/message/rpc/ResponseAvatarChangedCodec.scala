package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.ResponseAvatarChanged
import com.secretapp.backend.proto
import com.secretapp.backend.models
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
    protoState.encode(r.state) match {
      case \/-(bytesState) =>
        val boxed = protobuf.ResponseAvatarChanged(proto.toProto(r.avatar), r.seq, bytesState)
        encodeToBitVector(boxed)
      case l @ -\/(_) => l
    }

  }

  def decode(buf: BitVector) = {
    decodeProtobufEither(protobuf.ResponseAvatarChanged.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseAvatarChanged(avatar, seq, bytesState)) =>
        protoState.decodeValue(bytesState) match {
          case \/-(state) =>
            ResponseAvatarChanged(proto.fromProto(avatar), seq, state).right
          case l @ -\/(_) => l
        }
    }
  }
}
