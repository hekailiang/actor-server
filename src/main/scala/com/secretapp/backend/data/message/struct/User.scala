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
                avatar: Option[Avatar] = None,
                localName: Option[String] = None) extends ProtobufMessage {

  lazy val toProto = protobuf.User(
    uid,
    accessHash,
    name,
    localName,
    sex.map(_.toProto),
    keyHashes.toIndexedSeq,
    phoneNumber,
    avatar.map(_.toProto)
  )
}

object User {
  def fromProto(u: protobuf.User): User = u match {
    case protobuf.User(uid, accessHash, name, localName, sex, keyHashes, phoneNumber, avatar) =>
      User(uid, accessHash, name, sex.map(models.Sex.fromProto),
        keyHashes.toSet, phoneNumber, avatar.map(Avatar.fromProto), localName)
  }

  def fromModel(u: models.User, senderAuthId: Long)(implicit s: ActorSystem) = {
    val hash = ACL.userAccessHash(senderAuthId, u)
    User(u.uid, hash, u.name, u.sex.toOption, u.keyHashes, u.phoneNumber, u.avatar)
  }
}
