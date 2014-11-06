package com.secretapp.backend.data.message.rpc.history

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct
import scala.collection.immutable

@SerialVersionUID(1L)
case class ResponseDialogs(groups: immutable.Seq[struct.Group],
                           users: immutable.Seq[struct.User],
                           dialogs: immutable.Seq[Dialog]) extends RpcResponseMessage {
  val header = ResponseDialogs.header
}

object ResponseDialogs extends RpcResponseMessageObject {
  val header = 0x69
}
