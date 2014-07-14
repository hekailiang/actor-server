package com.secretapp.backend.protocol.codecs.message.rpc.messaging

import scala.collection.immutable.Seq
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
import com.getsecretapp.{ proto => protobuf }

object RequestSendMessageCodec extends Codec[RequestSendMessage] with utils.ProtobufCodec {
  def encode(r : RequestSendMessage) = {
    val boxed = protobuf.RequestSendMessage(r.uid, r.accessHash, r.randomId, r.useAesKey, r.aesMessage, r.messages.map(_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf : BitVector) = {
    decodeProtobuf(protobuf.RequestSendMessage.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestSendMessage(uid, accessHash, randomId, useAesKey, aesMessage, messages)) =>
        RequestSendMessage(uid, accessHash, randomId, useAesKey, aesMessage, messages.map(EncryptedMessage.fromProto(_)))
    }
  }
}
