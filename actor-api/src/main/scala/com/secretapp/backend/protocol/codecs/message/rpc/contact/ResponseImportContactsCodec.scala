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

object ResponseImportContactsCodec extends Codec[ResponseImportContacts] with utils.ProtobufCodec {
  def encode(r: ResponseImportContacts) = {
    val boxed = protobuf.ResponseImportContacts(r.users.map(_.toProto), r.seq, stateOpt.encodeValid(r.state))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobufEither(protobuf.ResponseImportContacts.parseFrom(buf.toByteArray)) {
      case Success(r) =>
        stateOpt.decode(r.state) match {
          case \/-((_, state)) => ResponseImportContacts(r.users.map(User.fromProto), r.seq, state).right
          case -\/(e) => e.left
        }
    }
  }
}
