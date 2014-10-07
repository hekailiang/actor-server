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

@SerialVersionUID(1l)
case class EncryptedMessage(message: BitVector, keys: immutable.Seq[EncryptedKey]) extends ProtobufMessage
{
  def toProto = protobuf.EncryptedMessage(message, keys map (_.toProto))
}

object EncryptedMessage {
  def fromProto(u: protobuf.EncryptedMessage): EncryptedMessage = u match {
    case protobuf.EncryptedMessage(message, keys) =>
      EncryptedMessage(message, keys map EncryptedKey.fromProto)
  }
}
