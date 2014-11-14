package com.secretapp.backend.data.message.update

@SerialVersionUID(1L)
case class UserOnline(userId: Int) extends WeakUpdateMessage {
  val header = UserOnline.header
}

object UserOnline extends WeakUpdateMessageObject {
  val header = 0x07
}
