package com.secretapp.backend.protocol.codecs.message.rpc.contact

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object RequestImportContactsCodec extends Codec[RequestImportContacts] with utils.ProtobufCodec {
  def encode(r: RequestImportContacts) = {
    val boxed = protobuf.RequestImportContacts(r.contacts.map(_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobufEither(protobuf.RequestImportContacts.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestImportContacts(contacts)) =>
        if (contacts.length > 1000) {
          "TOO_MANY".left
        } else {
          RequestImportContacts(contacts.map(ContactToImport.fromProto(_))).right
        }
      }
    }
}
