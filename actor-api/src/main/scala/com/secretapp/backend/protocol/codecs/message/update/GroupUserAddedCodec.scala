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

object GroupUserAddedCodec extends Codec[GroupUserAdded] with utils.ProtobufCodec {
  def encode(u: GroupUserAdded) = {
    val boxed = protobuf.UpdateGroupUserAdded(u.groupId, u.userId, u.inviterUserId, u.date)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateGroupUserAdded.parseFrom(buf.toByteArray)) {
      case Success(
        protobuf.UpdateGroupUserAdded(
          groupId, userId, inviterUserId, date
        )
      ) =>
        GroupUserAdded(
          groupId, userId, inviterUserId, date
        )
    }
  }
}
