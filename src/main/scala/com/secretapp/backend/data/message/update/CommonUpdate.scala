package com.secretapp.backend.data.message.update

case class CommonUpdate(seq : Int, state : List[Byte], updateId : Int, update : List[Byte]) extends UpdateMessage
object CommonUpdate {
  val header = 0xd
}
