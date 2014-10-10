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

object RequestInviteUsersCodec extends Codec[RequestInviteUsers] with utils.ProtobufCodec {
  def encode(r: RequestInviteUsers) = {
    val boxed = protobuf.RequestInviteUsers(r.chatId, r.accessHash, r.randomId, r.chatKeyHash, r.broadcast.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestInviteUsers.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestInviteUsers(chatId, accessHash, randomId, chatKeyHash, broadcast)) =>
        RequestInviteUsers(chatId, accessHash, randomId, chatKeyHash, EncryptedRSABroadcast.fromProto(broadcast))
    }
  }
}
