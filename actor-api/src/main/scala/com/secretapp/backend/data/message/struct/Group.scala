package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.proto
import com.secretapp.backend.models
import im.actor.messenger.{ api => protobuf }
import scalaz._
import Scalaz._

@SerialVersionUID(1L)
case class Group(
  id: Int,
  accessHash: Long,
  title: String,
  avatar: Option[models.Avatar] = None
) extends ProtobufMessage {
  lazy val toProto = protobuf.Group(
    id,
    accessHash,
    title,
    avatar map proto.toProto[models.Avatar, protobuf.Avatar])
}

object Group {
  def fromProto(g: protobuf.Group): Group = g match {
    case protobuf.Group(id, accessHash, title, avatar) =>
      Group(id, accessHash, title, avatar map proto.fromProto[models.Avatar, protobuf.Avatar])
  }
}
