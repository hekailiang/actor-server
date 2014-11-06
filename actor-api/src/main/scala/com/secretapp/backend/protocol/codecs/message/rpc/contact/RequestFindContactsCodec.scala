package com.secretapp.backend.protocol.codecs.message.rpc.contact

import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{api => protobuf}
import scodec.Codec
import scodec.bits._
import scala.util.Success

object RequestFindContactsCodec extends Codec[RequestFindContacts] with utils.ProtobufCodec {
  def encode(r: RequestFindContacts) = {
    val boxed = protobuf.RequestFindContacts(r.request)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestFindContacts.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestFindContacts(request)) => RequestFindContacts(request)
    }
  }
}
