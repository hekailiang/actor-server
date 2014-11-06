package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.data.message.struct
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object RequestDeleteGroupCodec extends Codec[RequestDeleteGroup] with utils.ProtobufCodec {
  def encode(r: RequestDeleteGroup) = {
    val boxed = protobuf.RequestDeleteGroup(r.groupPeer.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestDeleteGroup.parseFrom(buf.toByteArray)) {
      case Success(r) => RequestDeleteGroup(struct.GroupOutPeer.fromProto(r.groupPeer))
    }
  }
}
