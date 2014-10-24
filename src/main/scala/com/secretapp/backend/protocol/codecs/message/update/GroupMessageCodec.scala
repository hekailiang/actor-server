package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.rpc.messaging.EncryptedAESPackage
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object GroupMessageCodec extends Codec[GroupMessage] with utils.ProtobufCodec {
  def encode(u: GroupMessage) = {
    val boxed = protobuf.UpdateGroupMessage(
      u.senderUID, u.groupId, u.message.toProto
    )
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateGroupMessage.parseFrom(buf.toByteArray)) {
      case Success(
        protobuf.UpdateGroupMessage(
          senderUID, groupId, message
        )
      ) =>
        GroupMessage(
          senderUID, groupId, EncryptedAESPackage.fromProto(message)
        )
    }
  }
}
