package com.secretapp.backend.data.message.rpc.messaging

import scala.language.implicitConversions
import com.secretapp.backend.data.types
import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{ api => protobuf }
import scodec.bits.BitVector
import scalaz._
import Scalaz._

@SerialVersionUID(1l)
case class EncryptedKey(keyHash: Long, aesEncryptedKey: BitVector) extends ProtobufMessage {
  def toProto = protobuf.EncryptedKey(keyHash, aesEncryptedKey)
}

object EncryptedKey {
  def fromProto(u: protobuf.EncryptedKey): EncryptedKey = u match {
    case protobuf.EncryptedKey(keyHash, aesEncryptedKey) =>
      EncryptedKey(keyHash, aesEncryptedKey)
  }
}
