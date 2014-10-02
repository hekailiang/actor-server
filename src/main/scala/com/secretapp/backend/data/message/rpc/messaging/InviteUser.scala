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

case class InviteUser (
  uid: Int,
  accessHash: Long,
  keys: immutable.Seq[EncryptedMessage]
) extends ProtobufMessage {
  def toProto = protobuf.InviteUser(uid, accessHash, keys map (_.toProto))
}

object InviteUser {
  def fromProto(m: protobuf.InviteUser): InviteUser = m match {
    case protobuf.InviteUser(uid, accessHash, keys) =>
      InviteUser(uid, accessHash, keys map EncryptedMessage.fromProto)
  }
}
