package com.secretapp.backend.data.message.rpc.messaging

import scala.language.implicitConversions
import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.api.{ RequestSendEncryptedMessage => protobuf }
import scodec.bits.BitVector
import scala.collection.immutable
import scalaz._
import Scalaz._

@SerialVersionUID(1L)
case class EncryptedAESKey(
  keyHash: Long,
  aesEncryptedKey: BitVector
) extends ProtobufMessage {
  def toProto = protobuf.EncryptedAESKey(keyHash, aesEncryptedKey)
}

object EncryptedAESKey {
  def fromProto(k: protobuf.EncryptedAESKey): EncryptedAESKey = EncryptedAESKey(k.keyHash, k.aesEncryptedKey)
}
