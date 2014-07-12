package com.secretapp.backend.data.message.rpc

case class Ok(responseType : Int, body : RpcResponseMessage)
object Ok {
  val header = 0x1
}
