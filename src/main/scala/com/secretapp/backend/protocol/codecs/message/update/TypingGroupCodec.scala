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

object TypingGroupCodec extends Codec[TypingGroup] with utils.ProtobufCodec {
  def encode(u: TypingGroup) = {
    val boxed = protobuf.UpdateTypingGroup(u.chatId, u.uid, u.typingType)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateTypingGroup.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateTypingGroup(chatId, uid, typingType)) => TypingGroup(chatId, uid, typingType)
    }
  }
}
