package com.secretapp.backend.protocol.codecs.message.rpc.contact

import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import im.actor.messenger.{api => protobuf}
import scodec.Codec
import scodec.bits._
import scala.util.Success

object ResponseFindContactsCodec extends Codec[ResponseFindContacts] with utils.ProtobufCodec {
  def encode(r: ResponseFindContacts) = {
    val boxed = protobuf.ResponseFindContacts(r.users.map(_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseFindContacts.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseFindContacts(users)) => ResponseFindContacts(users.map(struct.User.fromProto))
    }
  }
}
