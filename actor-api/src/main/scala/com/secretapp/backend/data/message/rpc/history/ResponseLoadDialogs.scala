package com.secretapp.backend.data.message.rpc.history

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct
import scala.collection.immutable

@SerialVersionUID(1L)
case class ResponseLoadDialogs(groups: immutable.Seq[struct.Group],
                           users: immutable.Seq[struct.User],
                           dialogs: immutable.Seq[Dialog]) extends RpcResponseMessage {
  val header = ResponseLoadDialogs.header
}

object ResponseLoadDialogs extends RpcResponseMessageObject {
  val header = 0x69
}
