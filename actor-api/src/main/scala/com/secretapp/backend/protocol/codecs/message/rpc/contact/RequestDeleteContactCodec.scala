package com.secretapp.backend.protocol.codecs.message.rpc.contact

import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{api => protobuf}
import scodec.Codec
import scodec.bits._
import scala.util.Success

object RequestDeleteContactCodec extends Codec[RequestDeleteContact] with utils.ProtobufCodec {
  def encode(r: RequestDeleteContact) = {
    val boxed = protobuf.RequestDeleteContact(r.uid, r.accessHash)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestDeleteContact.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestDeleteContact(uid, accessHash)) => RequestDeleteContact(uid, accessHash)
    }
  }
}
