package com.secretapp.backend.data.message.struct

import akka.actor.ActorSystem
import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.models
import com.secretapp.backend.proto
import com.secretapp.backend.util.ACL
import im.actor.messenger.{ api => protobuf }
import scala.collection.immutable
import scala.language.implicitConversions
import scalaz._
import Scalaz._

@SerialVersionUID(1L)
case class User(
  uid: Int,
  accessHash: Long,
  name: String,
  sex: Option[models.Sex],
  keyHashes: Set[Long],
  phoneNumber: Long,
  phoneIds: immutable.Set[Int],
  emailIds: immutable.Set[Int],
  state: models.UserState,
  avatar: Option[models.Avatar],
  localName: Option[String]
) extends ProtobufMessage {

  protected def sexToProto(s: models.Sex): protobuf.Sex.EnumVal = s match {
    case models.Male => protobuf.Sex.MALE
    case models.Female => protobuf.Sex.FEMALE
    case models.NoSex => protobuf.Sex.UNKNOWN
  }

  protected def stateToProto(s: models.UserState): protobuf.UserState.EnumVal = s match {
    case models.UserState.Registered => protobuf.UserState.REGISTERED
    case models.UserState.Email => protobuf.UserState.EMAIL
    case models.UserState.Deleted => protobuf.UserState.DELETED
  }

  lazy val toProto = protobuf.User(
    uid,
    accessHash,
    name,
    localName,
    sex.map(sexToProto),
    keyHashes.toIndexedSeq,
    phoneNumber,
    avatar map proto.toProto[models.Avatar, protobuf.Avatar],
    phoneIds.toIndexedSeq,
    emailIds.toIndexedSeq,
    stateToProto(state)
  )
}

object User {
  protected def fromProto(pb: protobuf.Sex.EnumVal): models.Sex = pb match {
    case protobuf.Sex.MALE => models.Male
    case protobuf.Sex.FEMALE => models.Female
    case _ => models.NoSex
  }

  protected def fromProto(pb: protobuf.UserState.EnumVal): models.UserState = pb match {
    case protobuf.UserState.REGISTERED => models.UserState.Registered
    case protobuf.UserState.EMAIL => models.UserState.Email
    case protobuf.UserState.DELETED => models.UserState.Deleted
    case _ =>
      throw new Exception("Unknown user state")
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
      phoneIds = u.phones.toSet,
      emailIds = u.emails.toSet,
      state = fromProto(u.userState),
      avatar = u.avatar map proto.fromProto[models.Avatar, protobuf.Avatar]
    )

  private def toLocalName(localName: Option[String]) = localName match {
    case n @ Some(name) if name.nonEmpty => n
    case _ => None
  }

  def fromModel(u: models.User, ad: models.AvatarData, senderAuthId: Long, localName: Option[String])(implicit s: ActorSystem) = {
    User(
      uid = u.uid,
      accessHash = ACL.userAccessHash(senderAuthId, u),
      name = u.name,
      localName = toLocalName(localName),
      sex = u.sex.toOption,
      keyHashes = u.publicKeyHashes,
      phoneNumber = u.phoneNumber,
      phoneIds = u.phoneIds,
      emailIds = u.emailIds,
      state = u.state,
      avatar = ad.avatar
    )
  }

  def fromData(ud: models.UserData, ad: models.AvatarData, senderAuthId: Long, localName: Option[String])(implicit s: ActorSystem) = {
    User(
      uid = ud.id,
      accessHash = ACL.userAccessHash(senderAuthId, ud.id, ud.accessSalt),
      name = ud.name,
      localName = toLocalName(localName),
      sex = ud.sex.toOption,
      keyHashes = ud.publicKeyHashes,
      phoneNumber = ud.phoneNumber,
      phoneIds = ud.phoneIds,
      emailIds = ud.emailIds,
      state = ud.state,
      avatar = ad.avatar
    )
  }
}
