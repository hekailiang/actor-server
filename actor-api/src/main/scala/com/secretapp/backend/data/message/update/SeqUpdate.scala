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

@SerialVersionUID(1L)
case class SeqUpdate(seq: Int, state: BitVector, body: SeqUpdateMessage) extends UpdateMessage
{
  val header = SeqUpdate.header

  def toProto: String \/ protobuf.SeqUpdate = {
    for {
      update <- SeqUpdateMessageCodec.encode(body)
    } yield protobuf.SeqUpdate(seq, state, body.header, update)
  }
}

object SeqUpdate extends UpdateMessageObject {
  val header = 0x0D

  def fromProto(u: protobuf.SeqUpdate) = {
    for { update <- SeqUpdateMessageCodec.decode(u.updateHeader, u.update) }
    yield SeqUpdate(u.seq, u.state, update)
  }
}
