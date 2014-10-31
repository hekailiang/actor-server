package com.secretapp.backend.protocol.codecs.message.rpc.contact

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.struct.User
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object ResponseImportedContactsCodec extends Codec[ResponseImportedContacts] with utils.ProtobufCodec {
  def encode(r: ResponseImportedContacts) = {
    val boxed = protobuf.ResponseImportedContacts(r.users.map(_.toProto), r.seq, r.state)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseImportedContacts.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseImportedContacts(users, seq, state)) =>
        ResponseImportedContacts(users.map(User.fromProto), seq, state)
    }
  }
}
