package com.secretapp.backend.data.message.update

import com.secretapp.backend.protocol.codecs.message.update.WeakUpdateMessageCodec
import im.actor.messenger.{ api => protobuf }
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scalaz._
import Scalaz._

@SerialVersionUID(1L)
case class WeakUpdate(date: Long, body: WeakUpdateMessage) extends UpdateMessage {
  val header = WeakUpdate.header

  def toProto: String \/ protobuf.WeakUpdate = {
    for {
      update <- WeakUpdateMessageCodec.encode(body)
    } yield protobuf.WeakUpdate(date, body.header, update)
  }
}

object WeakUpdate extends UpdateMessageObject {
  val header = 0x1A

  def fromProto(u: protobuf.WeakUpdate) = {
    for { update <- WeakUpdateMessageCodec.decode(u.updateId, u.update) }
    yield WeakUpdate(u.date, update)
  }
}
