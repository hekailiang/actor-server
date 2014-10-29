package com.secretapp.backend.data.message.struct

import akka.actor.ActorSystem
import com.secretapp.backend.util.ACL

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
                avatar: Option[Avatar] = None) extends ProtobufMessage {

  protected def sexToProto(s: models.Sex): protobuf.Sex.EnumVal = s match {
    case models.Male => protobuf.Sex.MALE
    case models.Female => protobuf.Sex.FEMALE
    case models.NoSex => protobuf.Sex.UNKNOWN
  }

  lazy val toProto = protobuf.User(
    uid,
    accessHash,
    name,
    sex.map(sexToProto),
    keyHashes.toIndexedSeq,
    phoneNumber,
    avatar.map(_.toProto))
}

object User {
  protected def fromProto(pb: protobuf.Sex.EnumVal): models.Sex = pb match {
    case protobuf.Sex.MALE => models.Male
    case protobuf.Sex.FEMALE => models.Female
    case _ => models.NoSex
  }

  def fromProto(u: protobuf.User): User = u match {
    case protobuf.User(uid, accessHash, name, sex, keyHashes, phoneNumber, avatar) =>
      User(uid, accessHash, name, sex.map(fromProto(_)), keyHashes.toSet, phoneNumber, avatar.map(Avatar.fromProto))
  }

  def fromModel(u: models.User, senderAuthId: Long)(implicit s: ActorSystem) = {
    val hash = ACL.userAccessHash(senderAuthId, u)
    User(u.uid, hash, u.name, u.sex.toOption, u.keyHashes, u.phoneNumber, u.avatar)
  }
}
