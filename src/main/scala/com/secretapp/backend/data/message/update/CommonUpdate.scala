package com.secretapp.backend.data.message.update

import scala.language.implicitConversions
import com.secretapp.backend.protocol.codecs.message.UpdateMessageCodec
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.protocol.codecs.message.update._
import com.getsecretapp.{ proto => protobuf }
import scodec.bits.BitVector
import scalaz._
import Scalaz._

case class CommonUpdate(seq: Int, state: BitVector, body: UpdateMessage) extends UpdateMessage
{
  val updateType = 0xd

  def toProto: String \/ protobuf.CommonUpdate = {
    for {
      update <- UpdateMessageCodec.encode(body)
    } yield protobuf.CommonUpdate(seq, state, body.updateType, update)
  }
}

object CommonUpdate extends UpdateMessageObject {
  val updateType = 0xd

  def fromProto(u: protobuf.CommonUpdate): String \/ CommonUpdate = u match {
    case protobuf.CommonUpdate(seq, state, updateId, body) =>
      for {
        update <- UpdateMessageCodec.decode(updateId, body)
      } yield CommonUpdate(seq, state, update)
  }
}
