package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

case class Difference(seq : Int, state : List[Byte], user : struct.User, updates : CommonUpdate) extends RpcRequestMessage
object Difference extends RpcResponseMessageObject {
  val responseType = 0xc
}
