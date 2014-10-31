package com.secretapp.backend.data.message.update.contact

import com.secretapp.backend.data.message.update._
import im.actor.messenger.{ api => protobuf }
import scala.collection.immutable

@SerialVersionUID(1L)
case class UpdateContactsAdded(uids: immutable.Seq[Int]) extends SeqUpdateMessage {
  val header = UpdateContactsAdded.header

  def userIds: Set[Int] = uids.toSet

  def toProto = protobuf.UpdateContactsAdded(uids)
}

object UpdateContactsAdded extends SeqUpdateMessageObject {
  val header = 0x28
}
