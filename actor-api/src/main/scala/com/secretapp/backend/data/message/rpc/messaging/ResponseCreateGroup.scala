package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.rpc._
import java.util.UUID
import scala.collection.immutable

@SerialVersionUID(1L)
case class ResponseCreateGroup(groupPeer: struct.GroupOutPeer, seq: Int, state: Option[UUID],
                               users: immutable.Seq[Int]) extends RpcResponseMessage {
  val header = ResponseCreateGroup.header
}

object ResponseCreateGroup extends RpcResponseMessageObject {
  val header = 0x42
}
