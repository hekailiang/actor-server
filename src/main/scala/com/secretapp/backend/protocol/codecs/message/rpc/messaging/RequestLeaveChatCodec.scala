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

object RequestLeaveChatCodec extends Codec[RequestLeaveChat] with utils.ProtobufCodec {
  def encode(r: RequestLeaveChat) = {
    val boxed = protobuf.RequestLeaveChat(r.chatId, r.accessHash)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestLeaveChat.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestLeaveChat(chatId, accessHash)) =>
        RequestLeaveChat(chatId, accessHash)
    }
  }
}
