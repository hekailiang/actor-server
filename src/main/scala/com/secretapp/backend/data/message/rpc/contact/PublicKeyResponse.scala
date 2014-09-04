package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits.BitVector
import com.reactive.messenger.{ api => protobuf }

case class PublicKeyResponse(uid: Int, keyHash: Long, key: BitVector) extends ProtobufMessage
{
  def toProto = protobuf.PublicKey(uid, keyHash, key)
}

object PublicKeyResponse {
  def fromProto(r: protobuf.PublicKey): PublicKeyResponse = r match {
    case protobuf.PublicKey(uid, keyHash, key) => PublicKeyResponse(uid, keyHash, key)
  }
}
