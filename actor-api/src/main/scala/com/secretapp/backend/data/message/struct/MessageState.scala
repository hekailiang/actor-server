package com.secretapp.backend.data.message.struct

import im.actor.messenger.api.{ MessageState => protobuf }

sealed trait MessageState {
  val value: protobuf.EnumVal

  def toProto = value

  val intType: Int
}

object MessageState {
  @SerialVersionUID(1L)
  case object Sent extends MessageState {
    val value = protobuf.SENT

    val intType = value.id
  }

  @SerialVersionUID(1L)
  case object Received extends MessageState {
    val value = protobuf.RECEIVED

    val intType = value.id
  }

  @SerialVersionUID(1L)
  case object Read extends MessageState {
    val value = protobuf.READ

    val intType = value.id
  }

  def fromInt(id: Int) = {
    fromProto(protobuf.valueOf(id))
  }

  def fromProto(v: protobuf.EnumVal) = v match {
    case protobuf.SENT => Sent
    case protobuf.RECEIVED => Received
    case protobuf.READ => Read
    case _ => throw new IllegalArgumentException(s"Unknown message state: $v")
  }
}
