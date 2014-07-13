package com.secretapp.backend.data.message.rpc.update

import scala.language.implicitConversions
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.getsecretapp.{ proto => protobuf }

case class CommonUpdate(seq : Int, state : List[Byte], updateId : Int, update : List[Byte]) extends RpcResponseMessage
{
  def toProto = protobuf.CommonUpdate(seq, state, updateId, update)
}

object CommonUpdate extends RpcResponseMessageObject {
  val responseType = 0xd

  def fromProto(u : protobuf.CommonUpdate) : CommonUpdate = u match {
    case protobuf.CommonUpdate(seq, state, updateId, update) =>
      CommonUpdate(seq, state, updateId, update)
  }
}
