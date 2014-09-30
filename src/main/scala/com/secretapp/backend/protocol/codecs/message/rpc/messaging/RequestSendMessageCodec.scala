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

object RequestSendMessageCodec extends Codec[RequestSendMessage] with utils.ProtobufCodec {
  def encode(r: RequestSendMessage) = {
    val boxed = protobuf.RequestSendMessage(r.uid, r.accessHash, r.randomId, r.message.toProto, r.selfMessage map (_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestSendMessage.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestSendMessage(uid, accessHash, randomId, message, selfMessage)) =>
        RequestSendMessage(uid, accessHash, randomId, EncryptedMessage.fromProto(message), selfMessage map EncryptedMessage.fromProto)
    }
  }
}
