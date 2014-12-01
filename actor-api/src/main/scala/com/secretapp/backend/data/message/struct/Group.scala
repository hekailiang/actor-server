package com.secretapp.backend.data.message.struct

import akka.actor.ActorSystem
import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.util.ACL
import com.secretapp.backend.proto
import com.secretapp.backend.models
import im.actor.messenger.{ api => protobuf }
import scala.collection.immutable
import scalaz._
import Scalaz._

@SerialVersionUID(1L)
case class Group(
  id: Int,
  accessHash: Long,
  title: String,
  avatar: Option[models.Avatar],
  isMember: Boolean,
  adminUid: Int,
  members: immutable.Seq[Member],
  createDate: Long
) extends ProtobufMessage {
  lazy val toProto = protobuf.Group(
    id,
    accessHash,
    title,
    avatar map proto.toProto[models.Avatar, protobuf.Avatar],
    isMember,
    adminUid,
    members map (_.toProto),
    createDate
  )
}

object Group {
  def fromProto(g: protobuf.Group): Group =
    Group(
      g.id, g.accessHash, g.title, g.avatar.map(proto.fromProto[models.Avatar, protobuf.Avatar]),
      g.isMember, g.adminUid, g.members map Member.fromProto, g.createDate
    )

  def fromModel(group: models.Group, groupMembers: immutable.Seq[Member], isMember: Boolean, optAvatar: Option[models.Avatar])(implicit s: ActorSystem) = {
    Group(
      id = group.id,
      accessHash = group.accessHash,
      title = group.title,
      avatar = optAvatar,
      isMember = isMember,
      adminUid = group.creatorUserId,
      members = groupMembers,
      createDate = group.createDate
    )
  }
}
