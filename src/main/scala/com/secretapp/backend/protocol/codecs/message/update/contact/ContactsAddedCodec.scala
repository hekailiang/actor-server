package com.secretapp.backend.protocol.codecs.message.update.contact

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.update.contact._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object ContactsAddedCodec extends Codec[ContactsAdded] with utils.ProtobufCodec {
  def encode(u: ContactsAdded) = {
    val boxed = protobuf.UpdateContactsAdded(u.uids)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateContactsAdded.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateContactsAdded(uids)) => ContactsAdded(uids)
    }
  }
}
