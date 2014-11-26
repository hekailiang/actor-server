package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct
import scala.collection.immutable

@SerialVersionUID(1L)
case class ResponseGetAuthSessions(userAuths: immutable.Seq[struct.AuthSession]) extends RpcResponseMessage {
  val header = ResponseGetAuthSessions.header
}

object ResponseGetAuthSessions extends RpcResponseMessageObject {
  val header = 0x51
}
