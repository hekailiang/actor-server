package com.secretapp.backend.data.message.rpc.messaging

import scala.language.implicitConversions
import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{ api => protobuf }
import scodec.bits.BitVector
import scalaz._
import Scalaz._

@SerialVersionUID(1L)
case class EncryptedRSAPackage(
  keyHash: Long,
  aesEncryptedKey: BitVector,
  message: BitVector
) extends ProtobufMessage {
  def toProto = protobuf.EncryptedRSAPackage(keyHash, aesEncryptedKey, message)
}

object EncryptedRSAPackage {
  def fromProto(u: protobuf.EncryptedRSAPackage): EncryptedRSAPackage = u match {
    case protobuf.EncryptedRSAPackage(keyHash, aesEncryptedKey, message) =>
      EncryptedRSAPackage(keyHash, aesEncryptedKey, message)
  }
}
