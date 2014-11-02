package com.secretapp.backend.data.message.update.contact

import com.secretapp.backend.data.message.update._
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class ContactRegistered(uid: Int) extends SeqUpdateMessage {
  val header = ContactRegistered.header

  def userIds: Set[Int] = Set(uid)
}

object ContactRegistered extends SeqUpdateMessageObject {
  val header = 0x05
}
