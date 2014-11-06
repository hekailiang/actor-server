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

object GroupUserKickCodec extends Codec[GroupUserKick] with utils.ProtobufCodec {
  def encode(u: GroupUserKick) = {
    val boxed = protobuf.UpdateGroupUserKick(u.groupId, u.userId, u.kickerUid, u.date)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateGroupUserKick.parseFrom(buf.toByteArray)) {
      case Success(u) => GroupUserKick(u.groupId, u.uid, u.kickerUid, u.date)
    }
  }
}
