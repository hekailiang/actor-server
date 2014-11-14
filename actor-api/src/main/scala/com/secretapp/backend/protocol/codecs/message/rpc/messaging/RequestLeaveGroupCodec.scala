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

object RequestLeaveGroupCodec extends Codec[RequestLeaveGroup] with utils.ProtobufCodec {
  def encode(r: RequestLeaveGroup) = {
    val boxed = protobuf.RequestLeaveGroup(r.groupOutPeer.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestLeaveGroup.parseFrom(buf.toByteArray)) {
      case Success(r) => RequestLeaveGroup(struct.GroupOutPeer.fromProto(r.groupPeer))
    }
  }
}
