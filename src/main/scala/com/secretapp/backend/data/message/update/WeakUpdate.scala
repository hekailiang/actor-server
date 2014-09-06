package com.secretapp.backend.data.message.update

import com.secretapp.backend.protocol.codecs.message.update.WeakUpdateMessageCodec
import com.reactive.messenger.{ api => protobuf }
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scalaz._
import Scalaz._

case class WeakUpdate(date: Long, body: WeakUpdateMessage) extends UpdateMessage {
  val updateType = 0x1A

  def toProto: String \/ protobuf.WeakUpdate = {
    for {
      update <- WeakUpdateMessageCodec.encode(body)
    } yield protobuf.WeakUpdate(date, body.weakUpdateType, update)
  }
}
