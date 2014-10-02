package com.secretapp.backend.data.message.rpc.update

import scala.language.implicitConversions
import com.secretapp.backend.protocol.codecs.message.update.SeqUpdateMessageCodec
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.protocol.codecs.message.update._
import im.actor.messenger.{ api => protobuf }
import scodec.bits.BitVector
import scalaz._
import Scalaz._

case class DifferenceUpdate(body: SeqUpdateMessage)
{
  def toProto: String \/ protobuf.DifferenceUpdate = {
    for {
      update <- SeqUpdateMessageCodec.encode(body)
    } yield protobuf.DifferenceUpdate(body.seqUpdateHeader, update)
  }
}

object DifferenceUpdate {
  def fromProto(u: protobuf.DifferenceUpdate): String \/ DifferenceUpdate = u match {
    case protobuf.DifferenceUpdate(header, body) =>
      for {
        update <- SeqUpdateMessageCodec.decode(header, body)
      } yield DifferenceUpdate(update)
  }
}
