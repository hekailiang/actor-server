package com.secretapp.backend.protocol.codecs.message.rpc.contact

import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import im.actor.messenger.{api => protobuf}
import scodec.Codec
import scodec.bits._
import scala.util.Success

object ResponseGetContactsCodec extends Codec[ResponseGetContacts] with utils.ProtobufCodec {
  def encode(r: ResponseGetContacts) = {
    val boxed = protobuf.ResponseGetContacts(r.users.map(_.toProto), r.isNotChanged)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseGetContacts.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseGetContacts(users, isNotChanged)) =>
        ResponseGetContacts(users.map(struct.User.fromProto), isNotChanged)
    }
  }
}
