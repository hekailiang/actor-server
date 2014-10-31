package com.secretapp.backend.data.message.update.contact

import com.secretapp.backend.data.message.update._
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class UpdateContactRegistered(uid: Int) extends SeqUpdateMessage {
  val header = UpdateContactRegistered.header

  def userIds: Set[Int] = Set(uid)

  def toProto = protobuf.UpdateContactRegistered(uid)
}

object UpdateContactRegistered extends SeqUpdateMessageObject {
  val header = 0x05

  def fromProto(u: protobuf.UpdateContactRegistered): UpdateContactRegistered = u match {
    case protobuf.UpdateContactRegistered(uid) => UpdateContactRegistered(uid)
  }
}
