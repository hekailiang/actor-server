package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.data.message.rpc.messaging.MessageContent
import com.secretapp.backend.data.message.struct.Peer
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

object EncryptedMessageCodec extends Codec[EncryptedMessage] with utils.ProtobufCodec {
  def encode(m: EncryptedMessage) = {
    val boxed = protobuf.UpdateEncryptedMessage(m.peer.toProto, m.senderUid, m.date, m.keyHash, m.aesEncryptedKey, m.message)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateEncryptedMessage.parseFrom(buf.toByteArray)) {
      case Success(u) =>
        EncryptedMessage(Peer.fromProto(u.peer), u.senderUid, u.keyHash, u.aesEncryptedKey, u.message, u.date)
    }
  }
}
