package com.secretapp.backend.data.message.rpc.update

import scala.collection.immutable.Seq
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct
import scodec.bits.BitVector

case class Difference(seq : Int, state : BitVector, users : Seq[struct.User], updates : Seq[CommonUpdate]) extends RpcResponseMessage
object Difference extends RpcResponseMessageObject {
  val responseType = 0xc
}
