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
case class EncryptedRSABroadcast(
  encryptedMessage: BitVector,
  keys: immutable.Seq[EncryptedUserAESKeys],
  ownKeys: immutable.Seq[EncryptedAESKey]
) extends ProtobufMessage {
  def toProto = protobuf.EncryptedRSABroadcast(
    encryptedMessage, keys map (_.toProto), ownKeys map (_.toProto)
  )
}

object EncryptedRSABroadcast {
  def fromProto(u: protobuf.EncryptedRSABroadcast): EncryptedRSABroadcast = u match {
    case protobuf.EncryptedRSABroadcast(encryptedMessage, keys, ownKeys) =>
      EncryptedRSABroadcast(
        encryptedMessage,
        keys map EncryptedUserAESKeys.fromProto,
        ownKeys map EncryptedAESKey.fromProto
      )
  }
}
