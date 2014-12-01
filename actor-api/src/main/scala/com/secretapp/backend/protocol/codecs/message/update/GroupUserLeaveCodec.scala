package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object GroupUserLeaveCodec extends Codec[GroupUserLeave] with utils.ProtobufCodec {
  def encode(u: GroupUserLeave) = {
    val boxed = protobuf.UpdateGroupUserLeave(u.groupId, u.randomId, u.userId, u.date)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateGroupUserLeave.parseFrom(buf.toByteArray)) {
      case Success(u) => GroupUserLeave(u.groupId, u.rid, u.uid, u.date)
    }
  }
}
