package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object GroupMembersUpdateCodec extends Codec[GroupMembersUpdate] with utils.ProtobufCodec {
  def encode(u: GroupMembersUpdate) = {
    val boxed = protobuf.UpdateGroupMembersUpdate(u.groupId, u.members map (_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateGroupMembersUpdate.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateGroupMembersUpdate(groupId, members)) =>
        GroupMembersUpdate(groupId, members map struct.Member.fromProto)
    }
  }
}
