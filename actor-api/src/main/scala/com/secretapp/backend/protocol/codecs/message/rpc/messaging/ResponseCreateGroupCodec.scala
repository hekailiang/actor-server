package com.secretapp.backend.protocol.codecs.message.rpc.messaging

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import scodec.bits.BitVector
import scodec.Codec
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object ResponseCreateGroupCodec extends Codec[ResponseCreateGroup] with utils.ProtobufCodec {
  def encode(r: ResponseCreateGroup) = {
    val boxed = protobuf.ResponseCreateGroup(r.groupPeer.toProto, r.seq, stateOpt.encodeValid(r.state), r.users)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobufEither(protobuf.ResponseCreateGroup.parseFrom(buf.toByteArray)) {
      case Success(r) =>
        stateOpt.decode(r.state) match {
          case \/-((_, state)) =>
            ResponseCreateGroup(struct.GroupOutPeer.fromProto(r.groupPeer), r.seq, state, r.users).right
          case -\/(e) => e.left
        }
    }
  }
}
