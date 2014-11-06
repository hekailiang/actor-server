package com.secretapp.backend.data.message.struct

import im.actor.messenger.api.{ PeerType => protobuf }

sealed trait PeerType {
  val value: protobuf.EnumVal

  def toProto = value
}

object PeerType {
  @SerialVersionUID(1L)
  case object Private extends PeerType {
    val value = protobuf.PRIVATE
  }

  @SerialVersionUID(1L)
  case object Group extends PeerType {
    val value = protobuf.GROUP
  }

  def fromProto(v: protobuf.EnumVal) = v match {
    case protobuf.PRIVATE => Private
    case protobuf.GROUP => Group
    case _ => throw new IllegalArgumentException(s"Unknown peer type: $v")
  }
}
