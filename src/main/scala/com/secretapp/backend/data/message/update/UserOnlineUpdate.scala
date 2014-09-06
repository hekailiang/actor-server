package com.secretapp.backend.data.message.update

case class UserOnlineUpdate(uid: Int) extends WeakUpdateMessage {
  val weakUpdateType = UserOnlineUpdate.weakUpdateType
}

object UserOnlineUpdate extends WeakUpdateMessageObject {
  val weakUpdateType = 0x07
}
