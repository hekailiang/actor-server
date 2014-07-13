package com.secretapp.backend.data.message.rpc.update

import scala.collection.immutable.Seq
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

case class Difference(seq : Int, state : List[Byte], users : Seq[struct.User], updates : Seq[CommonUpdate]) extends RpcResponseMessage
object Difference extends RpcResponseMessageObject {
  val responseType = 0xc
}
