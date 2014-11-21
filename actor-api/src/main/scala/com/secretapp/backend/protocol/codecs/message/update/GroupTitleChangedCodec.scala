package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import im.actor.messenger.{ api => protobuf }
import scala.util.Success

object GroupTitleChangedCodec extends Codec[GroupTitleChanged] with utils.ProtobufCodec {
  def encode(u: GroupTitleChanged) = {
    val boxed = protobuf.UpdateGroupTitleChanged(u.groupId, u.userId, u.title, u.date)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateGroupTitleChanged.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateGroupTitleChanged(groupId, uid, title, date)) =>
        GroupTitleChanged(groupId, uid, title, date)
    }
  }
}
