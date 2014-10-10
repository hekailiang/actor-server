package com.secretapp.backend.data.message.rpc.messaging

import scala.language.implicitConversions
import com.secretapp.backend.data.types
import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{ api => protobuf }
import scodec.bits.BitVector
import scala.collection.immutable
import scalaz._
import Scalaz._

@SerialVersionUID(1L)
case class EncryptedAESMessage(
  keyHash: BitVector,
  encryptedMessage: BitVector
) extends ProtobufMessage {
  def toProto = protobuf.EncryptedAESMessage(keyHash, encryptedMessage)
}

object EncryptedAESMessage {
  def fromProto(u: protobuf.EncryptedAESMessage): EncryptedAESMessage = u match {
    case protobuf.EncryptedAESMessage(keyHash, encryptedMessage) =>
      EncryptedAESMessage(keyHash, encryptedMessage)
  }
}
