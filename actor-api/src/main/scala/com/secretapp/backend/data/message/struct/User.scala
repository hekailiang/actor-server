package com.secretapp.backend.data.message.struct

import akka.actor.ActorSystem
import com.secretapp.backend.util.ACL
import com.secretapp.backend.proto
import scala.language.implicitConversions
import com.secretapp.backend.models
import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }
import scalaz._
import Scalaz._

@SerialVersionUID(1L)
case class User(uid: Int,
                accessHash: Long,
                name: String,
                sex: Option[models.Sex],
                keyHashes: Set[Long],
                phoneNumber: Long,
                avatar: Option[models.Avatar] = None,
                localName: Option[String] = None) extends ProtobufMessage {

  protected def sexToProto(s: models.Sex): protobuf.Sex.EnumVal = s match {
    case models.Male => protobuf.Sex.MALE
    case models.Female => protobuf.Sex.FEMALE
    case models.NoSex => protobuf.Sex.UNKNOWN
  }

  lazy val toProto = protobuf.User(
    uid,
    accessHash,
    name,
    localName,
    sex.map(sexToProto),
    keyHashes.toIndexedSeq,
    phoneNumber,
    avatar map proto.toProto[models.Avatar, protobuf.Avatar])
}

object User {
  protected def fromProto(pb: protobuf.Sex.EnumVal): models.Sex = pb match {
    case protobuf.Sex.MALE => models.Male
    case protobuf.Sex.FEMALE => models.Female
    case _ => models.NoSex
  }

  def fromProto(u: protobuf.User): User =
    User(
      uid = u.id,
      accessHash = u.accessHash,
      name = u.name,
      localName = toLocalName(u.localName),
      sex = u.sex.map(fromProto),
      keyHashes = u.keyHashes.toSet,
      phoneNumber = u.phone,
      avatar = u.avatar map proto.fromProto[models.Avatar, protobuf.Avatar]
    )

  private def toLocalName(localName: Option[String]) = localName match {
    case n @ Some(name) if name.nonEmpty => n
    case _ => None
  }

  def fromModel(u: models.User, senderAuthId: Long, localName: Option[String] = None)(implicit s: ActorSystem) = {
    User(
      uid = u.uid,
      accessHash = ACL.userAccessHash(senderAuthId, u),
      name = u.name,
      localName = toLocalName(localName),
      sex = u.sex.toOption,
      keyHashes = u.keyHashes,
      phoneNumber = u.phoneNumber,
      avatar = u.avatar
    )
  }
}
