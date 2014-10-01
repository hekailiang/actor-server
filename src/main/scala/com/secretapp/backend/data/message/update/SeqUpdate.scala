package com.secretapp.backend.data.message.update

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

case class SeqUpdate(seq: Int, state: BitVector, body: SeqUpdateMessage) extends UpdateMessage
{
  val updateHeader = SeqUpdate.updateHeader

  def toProto: String \/ protobuf.SeqUpdate = {
    for {
      update <- SeqUpdateMessageCodec.encode(body)
    } yield protobuf.SeqUpdate(seq, state, body.seqUpdateHeader, update)
  }
}

object SeqUpdate extends UpdateMessageObject {
  val updateHeader = 0x0D

  def fromProto(u: protobuf.SeqUpdate): String \/ SeqUpdate = u match {
    case protobuf.SeqUpdate(seq, state, updateId, body) =>
      for {
        update <- SeqUpdateMessageCodec.decode(updateId, body)
      } yield SeqUpdate(seq, state, update)
  }
}
