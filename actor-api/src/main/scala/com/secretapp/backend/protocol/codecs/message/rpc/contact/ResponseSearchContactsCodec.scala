package com.secretapp.backend.protocol.codecs.message.rpc.contact

import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import im.actor.messenger.{api => protobuf}
import scodec.Codec
import scodec.bits._
import scala.util.Success

object ResponseSearchContactsCodec extends Codec[ResponseSearchContacts] with utils.ProtobufCodec {
  def encode(r: ResponseSearchContacts) = {
    val boxed = protobuf.ResponseSearchContacts(r.users.map(_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseSearchContacts.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseSearchContacts(users)) => ResponseSearchContacts(users.map(struct.User.fromProto))
    }
  }
}
