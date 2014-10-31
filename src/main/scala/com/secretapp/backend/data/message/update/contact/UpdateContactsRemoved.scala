package com.secretapp.backend.data.message.update.contact

import com.secretapp.backend.data.message.update._
import im.actor.messenger.{ api => protobuf }
import scala.collection.immutable

@SerialVersionUID(1L)
case class UpdateContactsRemoved(uids: immutable.Seq[Int]) extends SeqUpdateMessage {
  val header = UpdateContactsRemoved.header

  def userIds: Set[Int] = uids.toSet

  def toProto = protobuf.UpdateContactsRemoved(uids)
}

object UpdateContactsRemoved extends SeqUpdateMessageObject {
  val header = 0x29
}
