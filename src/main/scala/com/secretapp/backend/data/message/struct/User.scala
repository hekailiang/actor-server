package com.secretapp.backend.data.message.struct

import scala.collection.immutable
import scala.language.implicitConversions
import com.secretapp.backend.data.types
import com.secretapp.backend.data.message.ProtobufMessage
import com.reactive.messenger.{ api => protobuf }
import scalaz._
import Scalaz._

case class User(uid: Int,
                accessHash: Long,
                name: String,
                sex: Option[types.Sex],
                keyHashes: immutable.Set[Long],
                phoneNumber: Long
) extends ProtobufMessage
{
  def toProto = protobuf.User(uid, accessHash, name, sex.flatMap(_.toProto.some),
    keyHashes.toIndexedSeq, phoneNumber, None)
}

object User {
  def fromProto(u: protobuf.User): User = u match {
    case protobuf.User(uid, accessHash, name, sex, keyHashes, phoneNumber, _) =>
      User(uid, accessHash, name, sex.flatMap(types.Sex.fromProto(_).some), keyHashes.toSet, phoneNumber)
  }
}
