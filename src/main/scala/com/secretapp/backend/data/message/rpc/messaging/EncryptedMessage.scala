package com.secretapp.backend.data.message.rpc.messaging

import scala.language.implicitConversions
import scala.collection.immutable.Seq
import com.secretapp.backend.data.types
import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.getsecretapp.{ proto => protobuf }
import scodec.bits.BitVector
import scalaz._
import Scalaz._

case class EncryptedMessage(uid: Int,
                            keyHash: Long,
                            aesEncryptedKey: Option[BitVector],
                            message: Option[BitVector]) extends ProtobufMessage
{
  def toProto = protobuf.EncryptedMessage(uid, keyHash, aesEncryptedKey, message)
}

object EncryptedMessage {
  def fromProto(u: protobuf.EncryptedMessage): EncryptedMessage = u match {
    case protobuf.EncryptedMessage(uid, keyHash, aesEncryptedKey, message) =>
      EncryptedMessage(uid, keyHash, aesEncryptedKey, message)
  }
}
