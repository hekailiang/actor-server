package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct
import scala.collection.immutable

@SerialVersionUID(1L)
case class ResponseGetAuth(userAuths: immutable.Seq[struct.AuthItem]) extends RpcResponseMessage {
  val header = ResponseGetAuth.header
}

object ResponseGetAuth extends RpcResponseMessageObject {
  val header = 0x51
}
