package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class Config(maxGroupSize: Int) extends ProtobufMessage {
  def toProto = protobuf.Config(maxGroupSize)
}

object Config {
  def fromProto(c: protobuf.Config) = Config(c.maxGroupSize)
}
