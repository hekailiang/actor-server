package com.secretapp.backend.data.message.update.contact

import com.secretapp.backend.data.message.update._
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class ContactRegistered(userId: Int, isSilent: Boolean, date: Long) extends SeqUpdateMessage {
  val header = ContactRegistered.header

  def userIds: Set[Int] = Set(userId)

  def groupIds: Set[Int] = Set.empty
}

object ContactRegistered extends SeqUpdateMessageObject {
  val header = 0x05
}
