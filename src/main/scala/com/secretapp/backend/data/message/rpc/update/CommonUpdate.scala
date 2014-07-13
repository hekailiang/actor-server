package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc._

case class CommonUpdate(seq : Int, state : List[Byte], updateId : Int, update : List[Byte]) extends RpcResponseMessage
object CommonUpdate extends RpcResponseMessageObject {
  val responseType = 0xd
}
