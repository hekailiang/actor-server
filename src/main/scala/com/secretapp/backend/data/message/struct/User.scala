package com.secretapp.backend.data.message.struct

import scala.language.implicitConversions
import scala.collection.immutable.Seq
import com.secretapp.backend.data.types
import com.secretapp.backend.data.message.ProtobufMessage
import com.getsecretapp.{ proto => protobuf }
import scalaz._
import Scalaz._

case class User(uid: Int,
                accessHash: Long,
                firstName: String,
                lastName: Option[String],
                sex: Option[types.Sex],
                keyHashes: Seq[Long]) extends ProtobufMessage
{
  def toProto = protobuf.User(uid, accessHash, firstName, lastName, sex.flatMap(_.toProto.some), keyHashes)
}

object User {
  def fromProto(u: protobuf.User): User = u match {
    case protobuf.User(uid, accessHash, firstName, lastName, sex, keyHashes) =>
      User(uid, accessHash, firstName, lastName, sex.flatMap(types.Sex.fromProto(_).some), keyHashes)
  }
}
