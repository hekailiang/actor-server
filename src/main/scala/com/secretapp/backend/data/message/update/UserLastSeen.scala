package com.secretapp.backend.data.message.update

case class UserLastSeen(uid: Int, time: Long) extends WeakUpdateMessage {
  val weakUpdateHeader = UserLastSeen.weakUpdateHeader
}

object UserLastSeen extends WeakUpdateMessageObject {
  val weakUpdateHeader = 0x09
}
