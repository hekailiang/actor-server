package com.secretapp.backend.data.message.update

case class UserOfflineUpdate(uid: Int) extends WeakUpdateMessage {
  val weakUpdateType = UserOnlineUpdate.weakUpdateType
}

object UserOfflineUpdate extends WeakUpdateMessageObject {
  val weakUpdateType = 0x08
}
