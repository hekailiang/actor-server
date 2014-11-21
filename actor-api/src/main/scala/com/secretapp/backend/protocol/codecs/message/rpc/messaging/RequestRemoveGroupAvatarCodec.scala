package com.secretapp.backend.protocol.codecs.message.rpc.messaging

import com.secretapp.backend.data.message.struct
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object RequestRemoveGroupAvatarCodec extends Codec[RequestRemoveGroupAvatar] with utils.ProtobufCodec {
  def encode(r: RequestRemoveGroupAvatar) = {
    val boxed = protobuf.RequestRemoveGroupAvatar(r.outPeer.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestRemoveGroupAvatar.parseFrom(buf.toByteArray)) {
      case Success(r) => RequestRemoveGroupAvatar(struct.GroupOutPeer.fromProto(r.groupPeer))
    }
  }
}
