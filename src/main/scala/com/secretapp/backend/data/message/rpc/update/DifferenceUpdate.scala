package com.secretapp.backend.data.message.rpc.update

import scala.language.implicitConversions
import com.secretapp.backend.protocol.codecs.message.update.CommonUpdateMessageCodec
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.protocol.codecs.message.update._
import com.secretapp.{ proto => protobuf }
import scodec.bits.BitVector
import scalaz._
import Scalaz._

case class DifferenceUpdate(body: CommonUpdateMessage) extends RpcResponseMessage
{
  def toProto: String \/ protobuf.DifferenceUpdate = {
    for {
      update <- CommonUpdateMessageCodec.encode(body)
    } yield protobuf.DifferenceUpdate(body.commonUpdateType, update)
  }
}

object DifferenceUpdate {
  def fromProto(u: protobuf.DifferenceUpdate): String \/ DifferenceUpdate = u match {
    case protobuf.DifferenceUpdate(updateId, body) =>
      for {
        update <- CommonUpdateMessageCodec.decode(updateId, body)
      } yield DifferenceUpdate(update)
  }
}
