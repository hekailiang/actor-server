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

object RequestInviteUserCodec extends Codec[RequestInviteUser] with utils.ProtobufCodec {
  def encode(r: RequestInviteUser) = {
    val boxed = protobuf.RequestInviteUser(r.chatId, r.accessHash, r.userId, r.userAccessHash, r.randomId, r.chatKeyHash, r.invite map (_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestInviteUser.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestInviteUser(chatId, accessHash, userId, userAccessHash, randomId, chatKeyHash, invite)) =>
        RequestInviteUser(chatId, accessHash, userId, userAccessHash, randomId, chatKeyHash, invite map EncryptedMessage.fromProto)
    }
  }
}
