package com.secretapp.backend.data.message.rpc.messaging

import scala.language.implicitConversions
import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{ api => protobuf }
import scala.collection.immutable
import scalaz._
import Scalaz._

@SerialVersionUID(1L)
case class EncryptedUserAESKeys(
  userId: Int,
  accessHash: Long,
  keys: immutable.Seq[EncryptedAESKey]
) extends ProtobufMessage {
  def toProto = protobuf.EncryptedUserAESKeys(
    userId, accessHash, keys map (_.toProto)
  )
}

object EncryptedUserAESKeys {
  def fromProto(u: protobuf.EncryptedUserAESKeys): EncryptedUserAESKeys = u match {
    case protobuf.EncryptedUserAESKeys(userId, accessHash, keys) =>
      EncryptedUserAESKeys(userId, accessHash, keys map EncryptedAESKey.fromProto)
  }
}
