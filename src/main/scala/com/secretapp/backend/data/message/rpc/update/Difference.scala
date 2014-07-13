package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc.RpcRequestMessage
import com.secretapp.backend.data.message.struct

case class Difference(seq : Int, state : List[Byte], user : struct.User, updates : CommonUpdate) extends RpcRequestMessage
object Difference {
  val header = 0xc
}
