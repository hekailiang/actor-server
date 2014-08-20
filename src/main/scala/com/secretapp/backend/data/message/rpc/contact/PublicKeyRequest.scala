package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.{ proto => protobuf }

case class PublicKeyRequest(uid: Int, accessHash: Long, keyHash: Long) extends ProtobufMessage
{
  def toProto = protobuf.PublicKeyRequest(uid, accessHash, keyHash)
}

object PublicKeyRequest {
  def fromProto(r: protobuf.PublicKeyRequest): PublicKeyRequest = r match {
    case protobuf.PublicKeyRequest(uid, accessHash, keyHash) => PublicKeyRequest(uid, accessHash, keyHash)
  }
}
