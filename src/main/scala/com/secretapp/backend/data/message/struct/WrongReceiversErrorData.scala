package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.rpc.ErrorData
import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class WrongReceiversErrorData(newKeys: Seq[UserKey], removedKeys: Seq[UserKey]) extends ProtobufMessage with ErrorData {
  def toProto = protobuf.WrongReceiversErrorData(removedKeys.toVector map (_.toProto), newKeys.toVector map (_.toProto))
}

object WrongReceiversErrorData {
  def fromProto(data: protobuf.WrongReceiversErrorData): WrongReceiversErrorData = data match {
    case protobuf.WrongReceiversErrorData(removedKeys, newKeys) =>
      WrongReceiversErrorData(removedKeys map UserKey.fromProto, newKeys map UserKey.fromProto)
  }
}
