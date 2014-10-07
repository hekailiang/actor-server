package com.secretapp.backend.data.message.update

import com.secretapp.backend.protocol.codecs.message.update.WeakUpdateMessageCodec
import im.actor.messenger.{ api => protobuf }
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scalaz._
import Scalaz._

@SerialVersionUID(1l)
case class WeakUpdate(date: Long, body: WeakUpdateMessage) extends UpdateMessage {
  val updateHeader = WeakUpdate.updateHeader

  def toProto: String \/ protobuf.WeakUpdate = {
    for {
      update <- WeakUpdateMessageCodec.encode(body)
    } yield protobuf.WeakUpdate(date, body.weakUpdateHeader, update)
  }
}

object WeakUpdate extends UpdateMessageObject {
  val updateHeader = 0x1A

  def fromProto(u: protobuf.WeakUpdate): String \/ WeakUpdate = u match {
    case protobuf.WeakUpdate(date, updateId, body) =>
      for {
        update <- WeakUpdateMessageCodec.decode(updateId, body)
      } yield WeakUpdate(date, update)
  }
}
