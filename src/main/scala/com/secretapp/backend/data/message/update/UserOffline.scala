package com.secretapp.backend.data.message.update

@SerialVersionUID(1L)
case class UserOffline(uid: Int) extends WeakUpdateMessage {
  val weakUpdateHeader = UserOffline.weakUpdateHeader
}

object UserOffline extends WeakUpdateMessageObject {
  val weakUpdateHeader = 0x08
}
