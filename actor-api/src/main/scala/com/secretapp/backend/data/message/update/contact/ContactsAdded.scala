package com.secretapp.backend.data.message.update.contact

import com.secretapp.backend.data.message.update._
import im.actor.messenger.{ api => protobuf }
import scala.collection.immutable

@SerialVersionUID(1L)
case class ContactsAdded(uids: immutable.Seq[Int]) extends SeqUpdateMessage {
  val header = ContactsAdded.header

  def userIds: Set[Int] = uids.toSet

  def groupIds: Set[Int] = Set.empty
}

object ContactsAdded extends SeqUpdateMessageObject {
  val header = 0x28
}
