package com.secretapp.backend.protocol.codecs.message.rpc.contact

import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{api => protobuf}
import scodec.Codec
import scodec.bits._
import scala.util.Success

object RequestSearchContactsCodec extends Codec[RequestSearchContacts] with utils.ProtobufCodec {
  def encode(r: RequestSearchContacts) = {
    val boxed = protobuf.RequestSearchContacts(r.request)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestSearchContacts.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestSearchContacts(request)) => RequestSearchContacts(request)
    }
  }
}
