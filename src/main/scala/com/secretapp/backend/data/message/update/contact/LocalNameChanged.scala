package com.secretapp.backend.data.message.update.contact

import com.secretapp.backend.data.message.update._
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class LocalNameChanged(uid: Int, localName: Option[String]) extends SeqUpdateMessage {
  val header = LocalNameChanged.header

  def userIds: Set[Int] = Set(uid)
}

object LocalNameChanged extends SeqUpdateMessageObject {
  val header = 0x33
}
