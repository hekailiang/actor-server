package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct
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

case class FatSeqUpdate(seq: Int,
                        state: BitVector,
                        body: SeqUpdateMessage,
                        users: immutable.Seq[struct.User],
                        groups: immutable.Seq[struct.Group]) extends UpdateMessage {
  val header = SeqUpdate.header

  def toProto: String \/ protobuf.UpdateFatSeqUpdate = {
    for {
      update <- SeqUpdateMessageCodec.encode(body)
    } yield protobuf.UpdateFatSeqUpdate(seq, state, body.header, update, users map (_.toProto), groups map (_.toProto))
  }
}

object FatSeqUpdate extends UpdateMessageObject {
  val header = 0x49

  def fromProto(u: protobuf.UpdateFatSeqUpdate) = {
    for { update <- SeqUpdateMessageCodec.decode(u.updateHeader, u.update) }
    yield FatSeqUpdate(u.seq, u.state, update, u.users map struct.User.fromProto, u.groups map struct.Group.fromProto)
  }
}
