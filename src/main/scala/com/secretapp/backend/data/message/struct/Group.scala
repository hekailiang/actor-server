package com.secretapp.backend.data.message.struct

import scala.language.implicitConversions
import com.secretapp.backend.data.types
import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }
import scalaz._
import Scalaz._

@SerialVersionUID(1L)
case class Group(
  id: Int,
  accessHash: Long,
  title: String,
  avatar: Option[Avatar] = None
) extends ProtobufMessage {
  lazy val toProto = protobuf.Group(
    id,
    accessHash,
    title,
    avatar.map(_.toProto))
}

object Group {
  def fromProto(g: protobuf.Group): Group = g match {
    case protobuf.Group(id, accessHash, title, avatar) =>
      Group(id, accessHash, title, avatar.map(Avatar.fromProto))
  }
}
