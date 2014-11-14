package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits.BitVector
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class PublicKeyResponse(userId: Int, keyHash: Long, key: BitVector) extends ProtobufMessage
{
  def toProto = protobuf.PublicKey(userId, keyHash, key)
}

object PublicKeyResponse {
  def fromProto(r: protobuf.PublicKey): PublicKeyResponse = PublicKeyResponse(r.uid, r.keyHash, r.key)
}
