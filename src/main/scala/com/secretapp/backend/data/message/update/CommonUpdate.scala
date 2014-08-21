package com.secretapp.backend.data.message.update

import scala.language.implicitConversions
import com.secretapp.backend.protocol.codecs.message.update.CommonUpdateMessageCodec
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.protocol.codecs.message.update._
import com.getsecretapp.{ proto => protobuf }
import scodec.bits.BitVector
import scalaz._
import Scalaz._

case class CommonUpdate(seq: Int, state: BitVector, body: CommonUpdateMessage) extends UpdateMessage
{
  val updateType = 0xd

  def toProto: String \/ protobuf.CommonUpdate = {
    for {
      update <- CommonUpdateMessageCodec.encode(body)
    } yield protobuf.CommonUpdate(seq, state, body.commonUpdateType, update)
  }
}

object CommonUpdate extends UpdateMessageObject {
  val updateType = 0xd

  def fromProto(u: protobuf.CommonUpdate): String \/ CommonUpdate = u match {
    case protobuf.CommonUpdate(seq, state, updateId, body) =>
      for {
        update <- CommonUpdateMessageCodec.decode(updateId, body)
      } yield CommonUpdate(seq, state, update)
  }
}
