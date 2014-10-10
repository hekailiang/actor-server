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
case class EncryptedAESPackage(
  keyHash: BitVector,
  message: BitVector
) extends ProtobufMessage {
  def toProto = protobuf.EncryptedAESPackage(keyHash, message)
}

object EncryptedAESPackage {
  def fromProto(u: protobuf.EncryptedAESPackage): EncryptedAESPackage = u match {
    case protobuf.EncryptedAESPackage(keyHash, message) =>
      EncryptedAESPackage(keyHash, message)
  }
}
