package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class PublicKeyRequest(userId: Int, accessHash: Long, keyHash: Long) extends ProtobufMessage
{
  def toProto = protobuf.PublicKeyRequest(userId, accessHash, keyHash)
}

object PublicKeyRequest {
  def fromProto(r: protobuf.PublicKeyRequest): PublicKeyRequest = PublicKeyRequest(r.uid, r.accessHash, r.keyHash)
}
