package com.secretapp.backend.data.message.rpc.update

import java.util.UUID
import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

case class Difference(seq: Int,
                      state: Option[UUID],
                      users: immutable.Seq[struct.User],
                      updates: immutable.Seq[DifferenceUpdate],
                      needMore: Boolean) extends RpcResponseMessage {
  val header = Difference.responseType
}
object Difference extends RpcResponseMessageObject {
  val responseType = 0x0C
}
