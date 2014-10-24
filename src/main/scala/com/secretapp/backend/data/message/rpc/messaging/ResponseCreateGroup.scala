package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import java.util.UUID
import scodec.bits._

@SerialVersionUID(1L)
case class ResponseCreateGroup(
  groupId: Int, accessHash: Long, seq: Int, state: Option[UUID]
) extends RpcResponseMessage {
  val header = ResponseCreateGroup.header
}

object ResponseCreateGroup extends RpcResponseMessageObject {
  val header = 0x42
}
