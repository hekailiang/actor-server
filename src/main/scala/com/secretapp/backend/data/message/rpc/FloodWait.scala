package com.secretapp.backend.data.message.rpc

case class FloodWait(delay : Int) extends RpcMessage
object FloodWait {
  val header = 0x3
}
