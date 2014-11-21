package com.secretapp.backend.protocol.codecs.message.rpc.messaging

import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.protocol.codecs.utils
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{ api => protobuf }
import scodec.Codec
import scodec.bits.BitVector
import scala.util.Success
import scalaz.\/

object RequestSendEncryptedMessageCodec extends Codec[RequestSendEncryptedMessage] with utils.ProtobufCodec {
  def encode(r: RequestSendEncryptedMessage) = {
    val boxed = protobuf.RequestSendEncryptedMessage(r.outPeer.toProto, r.randomId, r.encryptedMessage,
      r.keys.map(_.toProto), r.ownKeys.map(_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestSendEncryptedMessage.parseFrom(buf.toByteArray)) {
      case Success(r) => RequestSendEncryptedMessage(struct.OutPeer.fromProto(r.peer), r.rid, r.encryptedMessage,
        r.keys.map(EncryptedAESKey.fromProto), r.ownKeys.map(EncryptedAESKey.fromProto))
    }
  }
}
