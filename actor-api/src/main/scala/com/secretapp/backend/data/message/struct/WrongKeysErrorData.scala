package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.rpc.ErrorData
import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class WrongKeysErrorData(newKeys: Seq[UserKey], removedKeys: Seq[UserKey], invalidKeys: Seq[UserKey]) extends ProtobufMessage with ErrorData {
  def toProto = protobuf.WrongKeysErrorData(
    newKeys.toVector map (_.toProto),
    removedKeys.toVector map (_.toProto),
    invalidKeys.toVector map (_.toProto)
  )
}

object WrongKeysErrorData {
  def fromProto(data: protobuf.WrongKeysErrorData) =
    WrongKeysErrorData(
      data.newKeys map UserKey.fromProto,
      data.removedKeys map UserKey.fromProto,
      data.invalidKeys map UserKey.fromProto
    )
}
