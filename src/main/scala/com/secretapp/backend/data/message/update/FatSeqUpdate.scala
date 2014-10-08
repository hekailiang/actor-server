package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.User
import scala.language.implicitConversions
import com.secretapp.backend.protocol.codecs.message.update.SeqUpdateMessageCodec
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.protocol.codecs.message.update._
import im.actor.messenger.{ api => protobuf }
import scala.collection.immutable
import scodec.bits.BitVector
import scalaz._
import Scalaz._

case class FatSeqUpdate(seq: Int, state: BitVector, body: SeqUpdateMessage, users: immutable.Seq[User]) extends UpdateMessage {
  val updateHeader = SeqUpdate.updateHeader

  def toProto: String \/ protobuf.FatSeqUpdate = {
    for {
      update <- SeqUpdateMessageCodec.encode(body)
    } yield protobuf.FatSeqUpdate(seq, state, body.seqUpdateHeader, update, users map (_.toProto))
  }
}

object FatSeqUpdate extends UpdateMessageObject {
  val updateHeader = 0x49

  def fromProto(u: protobuf.FatSeqUpdate): String \/ FatSeqUpdate = u match {
    case protobuf.FatSeqUpdate(seq, state, updateId, body, users) =>
      for {
        update <- SeqUpdateMessageCodec.decode(updateId, body)
      } yield FatSeqUpdate(seq, state, update, users map (User.fromProto))
  }
}
