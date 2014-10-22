package com.secretapp.backend.data.message.update

@SerialVersionUID(1L)
case class UserOffline(uid: Int) extends WeakUpdateMessage {
  val header = UserOffline.header
}

object UserOffline extends WeakUpdateMessageObject {
  val header = 0x08
}
