package com.secretapp.backend.data.message.update

@SerialVersionUID(1L)
case class UserLastSeen(uid: Int, time: Long) extends WeakUpdateMessage {
  val header = UserLastSeen.header
}

object UserLastSeen extends WeakUpdateMessageObject {
  val header = 0x09
}
