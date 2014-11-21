package com.secretapp.backend.data.message.struct

import im.actor.messenger.api.{ PeerType => protobuf }

sealed trait PeerType {
  val value: protobuf.EnumVal

  def toProto = value

  def intType: Int
}

object PeerType {
  @SerialVersionUID(1L)
  case object Private extends PeerType {
    val value = protobuf.PRIVATE

    def intType = value.id
  }

  @SerialVersionUID(1L)
  case object Group extends PeerType {
    val value = protobuf.GROUP

    def intType = value.id
  }

  def fromInt(id: Int) = {
    fromProto(protobuf.valueOf(id))
  }

  def fromProto(v: protobuf.EnumVal) = v match {
    case protobuf.PRIVATE => Private
    case protobuf.GROUP => Group
    case _ => throw new IllegalArgumentException(s"Unknown peer type: $v")
  }
}
