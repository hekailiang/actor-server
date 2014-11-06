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

object MessageReceivedCodec extends Codec[MessageReceived] with utils.ProtobufCodec {
  def encode(u: MessageReceived) = {
    val boxed = protobuf.UpdateMessageReceived(u.uid, u.randomId)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateMessageReceived.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateMessageReceived(uid, randomId)) => MessageReceived(uid, randomId)
    }
  }
}
