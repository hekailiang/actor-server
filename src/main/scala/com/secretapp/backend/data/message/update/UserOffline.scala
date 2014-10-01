package com.secretapp.backend.data.message.update

case class UserOffline(uid: Int) extends WeakUpdateMessage {
  val weakUpdateHeader = UserOffline.weakUpdateHeader
}

object UserOffline extends WeakUpdateMessageObject {
  val weakUpdateHeader = 0x08
}
