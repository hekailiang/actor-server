package com.secretapp.backend.protocol.codecs.message.rpc.contact

import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{api => protobuf}
import scodec.Codec
import scodec.bits._
import scala.util.Success

object RequestGetContactsCodec extends Codec[RequestGetContacts] with utils.ProtobufCodec {
  def encode(r: RequestGetContacts) = {
    val boxed = protobuf.RequestGetContacts(r.contactsHash)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestGetContacts.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestGetContacts(contactsHash)) => RequestGetContacts(contactsHash)
    }
  }
}
