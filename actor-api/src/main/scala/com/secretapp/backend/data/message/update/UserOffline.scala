package com.secretapp.backend.data.message.update

@SerialVersionUID(1L)
case class UserOffline(userId: Int) extends WeakUpdateMessage {
  val header = UserOffline.header
}

object UserOffline extends WeakUpdateMessageObject {
  val header = 0x08
}
