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

object MessageCodec extends Codec[Message] with utils.ProtobufCodec {
  def encode(m: Message) = {
    val boxed = protobuf.UpdateMessage(m.senderUID, m.destUID, m.keyHash, m.aesEncryptedKey, m.message)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateMessage.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateMessage(senderUID, destUID, keyHash, aesEncryptedKey, message)) =>
        Message(senderUID, destUID, keyHash, aesEncryptedKey, message)
    }
  }
}
