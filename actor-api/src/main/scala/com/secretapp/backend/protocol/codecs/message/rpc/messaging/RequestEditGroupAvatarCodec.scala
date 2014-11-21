package com.secretapp.backend.protocol.codecs.message.rpc.messaging

import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.models
import com.secretapp.backend.data.message.struct._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.proto
import scodec.bits._
import scodec.Codec
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object RequestEditGroupAvatarCodec extends Codec[RequestEditGroupAvatar] with utils.ProtobufCodec {
  def encode(r: RequestEditGroupAvatar) = {
    val boxed = protobuf.RequestEditGroupAvatar(r.groupPeer.toProto, proto.toProto(r.fileLocation))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestEditGroupAvatar.parseFrom(buf.toByteArray)) {
      case Success(r) =>
        RequestEditGroupAvatar(struct.GroupOutPeer.fromProto(r.groupPeer),
          proto.fromProto[models.FileLocation, protobuf.FileLocation](r.fileLocation))
    }
  }
}
