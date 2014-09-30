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

object MessageSentCodec extends Codec[MessageSent] with utils.ProtobufCodec {
  def encode(m: MessageSent) = {
    val boxed = protobuf.UpdateMessageSent(m.uid, m.randomId)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateMessageSent.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateMessageSent(uid, randomId)) => MessageSent(uid, randomId)
    }
  }
}
