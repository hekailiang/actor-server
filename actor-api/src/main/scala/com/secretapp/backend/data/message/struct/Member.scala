package com.secretapp.backend.data.message.struct

import akka.actor.ActorSystem
import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.proto
import com.secretapp.backend.models
import im.actor.messenger.{ api => protobuf }
import scala.collection.immutable
import scalaz._
import Scalaz._

@SerialVersionUID(1L)
case class Member(
  id: Int,
  inviterUserId: Int,
  date: Long
) extends ProtobufMessage {
  lazy val toProto = protobuf.Member(
    id,
    inviterUserId,
    date
  )
}

object Member {
  def fromProto(m: protobuf.Member): Member =
    Member(m.uid, m.inviterUid, m.date)
}
