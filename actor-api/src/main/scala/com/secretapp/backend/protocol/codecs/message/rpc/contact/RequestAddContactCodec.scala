package com.secretapp.backend.protocol.codecs.message.rpc.contact

import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{api => protobuf}
import scodec.Codec
import scodec.bits._
import scala.util.Success

object RequestAddContactCodec extends Codec[RequestAddContact] with utils.ProtobufCodec {
  def encode(r: RequestAddContact) = {
    val boxed = protobuf.RequestAddContact(r.userId, r.accessHash)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestAddContact.parseFrom(buf.toByteArray)) {
      case Success(r) => RequestAddContact(r.uid, r.accessHash)
    }
  }
}
