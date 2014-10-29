package com.secretapp.backend.data.message.rpc.messaging

import scala.language.implicitConversions
import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{ api => protobuf }
import scodec.bits.BitVector
import scala.collection.immutable
import scalaz._
import Scalaz._

@SerialVersionUID(1L)
case class EncryptedRSAMessage(
  encryptedMessage: BitVector,
  keys: immutable.Seq[EncryptedAESKey],
  ownKeys: immutable.Seq[EncryptedAESKey]
) extends ProtobufMessage {
  def toProto = protobuf.EncryptedRSAMessage(
    encryptedMessage,
    keys map (_.toProto),
    ownKeys map (_.toProto)
  )
}

object EncryptedRSAMessage {
  def fromProto(u: protobuf.EncryptedRSAMessage): EncryptedRSAMessage = u match {
    case protobuf.EncryptedRSAMessage(encryptedMessage, keys, ownKeys) =>
      EncryptedRSAMessage(
        encryptedMessage,
        keys map EncryptedAESKey.fromProto,
        ownKeys map EncryptedAESKey.fromProto
      )
  }
}
