package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.ProtobufMessage
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
}
