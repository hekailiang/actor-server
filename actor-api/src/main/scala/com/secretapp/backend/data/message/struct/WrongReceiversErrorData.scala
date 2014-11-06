package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.rpc.ErrorData
import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class WrongReceiversErrorData(newKeys: Seq[UserKey], removedKeys: Seq[UserKey], invalidKeys: Seq[UserKey]) extends ProtobufMessage with ErrorData {
  def toProto = protobuf.WrongReceiversErrorData(
    newKeys.toVector map (_.toProto),
    removedKeys.toVector map (_.toProto),
    invalidKeys.toVector map (_.toProto)
  )
}

object WrongReceiversErrorData {
  def fromProto(data: protobuf.WrongReceiversErrorData) =
    WrongReceiversErrorData(
      data.newKeys map UserKey.fromProto,
      data.removedKeys map UserKey.fromProto,
      data.invalidKeys map UserKey.fromProto
    )
}
