package com.secretapp.backend.data.message.update.contact

import com.secretapp.backend.data.message.update._
import im.actor.messenger.{ api => protobuf }
import scala.collection.immutable

@SerialVersionUID(1L)
case class ContactsRemoved(uids: immutable.Seq[Int]) extends SeqUpdateMessage {
  val header = ContactsRemoved.header

  def userIds: Set[Int] = uids.toSet
}

object ContactsRemoved extends SeqUpdateMessageObject {
  val header = 0x29
}
