package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.rpc.messaging.EncryptedRSAPackage
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
      u.chatId, u.accessHash,
      u.title, u.invite.toProto
    )
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateGroupCreated.parseFrom(buf.toByteArray)) {
      case Success(
        protobuf.UpdateGroupCreated(
          chatId, accessHash, title, invite
        )
      ) =>
        GroupCreated(
          chatId, accessHash, title, EncryptedRSAPackage.fromProto(invite)
        )
    }
  }
}
