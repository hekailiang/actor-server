package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.data.message.rpc.messaging.InviteUser
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

object GroupCreatedCodec extends Codec[GroupCreated] with utils.ProtobufCodec {
  def encode(u: GroupCreated) = {
    val boxed = protobuf.UpdateGroupCreated(
      u.randomId, u.chatId, u.accessHash,
      u.title, u.keyHash, u.invites map (_.toProto)
    )
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateGroupCreated.parseFrom(buf.toByteArray)) {
      case Success(
        protobuf.UpdateGroupCreated(
          randomId, chatId, accessHash, title, keyHash, invites
        )
      ) =>
        GroupCreated(
          randomId, chatId, accessHash, title, keyHash, invites map InviteUser.fromProto
        )
    }
  }
}
