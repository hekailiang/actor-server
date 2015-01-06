package com.secretapp.backend.data.message.struct

import akka.actor.ActorSystem
import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.models
import com.secretapp.backend.util.ACL
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class Phone(
  id: Int,
  accessHash: Long,
  number: Long,
  title: String
) extends ProtobufMessage {
  lazy val toProto = protobuf.Phone(
    id,
    accessHash,
    number,
    title
  )
}

object Phone {
  def fromProto(m: protobuf.Phone): Phone =
    Phone(m.id, m.accessHash, m.phone, m.phoneTitle)

  def fromModel(authId: Long, p: models.UserPhone)(implicit s: ActorSystem) = {
    Phone(
      id = p.id,
      accessHash = ACL.phoneAccessHash(authId, p),
      number = p.number,
      title = p.title
    )
  }
}
