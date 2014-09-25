package com.secretapp.backend.data.message.update

case class UserLastSeenUpdate(uid: Int, time: Long) extends WeakUpdateMessage {
  val weakUpdateType = UserLastSeenUpdate.weakUpdateType
}

object UserLastSeenUpdate extends WeakUpdateMessageObject {
  val weakUpdateType = 0x09
}
