package com.secretapp.backend.data.message.rpc.update

import java.util.UUID
import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class ResponseGetDifference(seq: Int,
  state: Option[UUID],
  users: immutable.Seq[struct.User],
  groups: immutable.Seq[struct.Group],
  updates: immutable.Seq[DifferenceUpdate],
  needMore: Boolean) extends RpcResponseMessage {
  val header = ResponseGetDifference.header
}
object ResponseGetDifference extends RpcResponseMessageObject {
  val header = 0x0C
}
