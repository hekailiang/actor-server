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

object RequestRemoveUserCodec extends Codec[RequestRemoveUser] with utils.ProtobufCodec {
  def encode(r: RequestRemoveUser) = {
    val boxed = protobuf.RequestRemoveUser(r.chatId, r.accessHash, r.userId, r.userAccessHash)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestRemoveUser.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestRemoveUser(chatId, accessHash, userId, userAccessHash)) =>
        RequestRemoveUser(chatId, accessHash, userId, userAccessHash)
    }
  }
}
