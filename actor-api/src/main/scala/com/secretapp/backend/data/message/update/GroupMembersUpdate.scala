package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct
import scala.collection.immutable

@SerialVersionUID(1L)
case class GroupMembersUpdate(groupId: Int, members: immutable.Seq[struct.Member]) extends SeqUpdateMessage {
  val header = GroupMembersUpdate.header

  def userIds: Set[Int] = members.map(_.id).toSet

  def groupIds: Set[Int] = Set(groupId)
}

object GroupMembersUpdate extends SeqUpdateMessageObject {
  val header = 0x2C
}
