package com.secretapp.backend.data.message.struct

import akka.actor.ActorSystem
import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.models
import com.secretapp.backend.util.ACL
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class Email(
  id: Int,
  accessHash: Long,
  email: String,
  title: String
) extends ProtobufMessage {
  lazy val toProto = protobuf.Email(
    id,
    accessHash,
    email,
    title
  )
}

object Email {
  def fromProto(m: protobuf.Email): Email =
    Email(m.id, m.accessHash, m.email, m.emailTitle)

  def fromModel(authId: Long, e: models.UserEmail)(implicit s: ActorSystem) = {
    Email(
      id = e.id,
      accessHash = ACL.emailAccessHash(authId, e),
      email = e.email,
      title = e.title
    )
  }
}
