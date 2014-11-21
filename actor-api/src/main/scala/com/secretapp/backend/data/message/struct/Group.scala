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
  isMember: Boolean,
  members: immutable.Seq[Int],
  adminUid: Int,
  avatar: Option[models.Avatar] = None
) extends ProtobufMessage {
  lazy val toProto = protobuf.Group(
    id,
    accessHash,
    title,
    avatar map proto.toProto[models.Avatar, protobuf.Avatar],
    isMember,
    members,
    adminUid)
}

object Group {
  def fromProto(g: protobuf.Group): Group =
    Group(g.id, g.accessHash, g.title, g.isMember, g.members, g.adminUid,
      g.avatar.map(proto.fromProto[models.Avatar, protobuf.Avatar]))

  def fromModel(group: models.Group, groupUserIds: immutable.Seq[Int], isMember: Boolean, optAvatar: Option[models.Avatar])(implicit s: ActorSystem) = {
    Group(
      id = group.id,
      accessHash = group.accessHash,
      title = group.title,
      isMember = isMember,
      members = groupUserIds,
      adminUid = group.creatorUserId,
      avatar = optAvatar
    )
  }
}
