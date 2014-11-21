package com.secretapp.backend.data.message.update

import scala.collection.immutable

@SerialVersionUID(1L)
case class GroupMembersUpdate(groupId: Int, members: immutable.Seq[Int]) extends SeqUpdateMessage {
  val header = GroupMembersUpdate.header

  def userIds: Set[Int] = members.toSet

  def groupIds: Set[Int] = Set(groupId)
}

object GroupMembersUpdate extends SeqUpdateMessageObject {
  val header = 0x2C
}
