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

object ContactsRemovedCodec extends Codec[ContactsRemoved] with utils.ProtobufCodec {
  def encode(u: ContactsRemoved) = {
    val boxed = protobuf.UpdateContactsRemoved(u.uids)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateContactsRemoved.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateContactsRemoved(uids)) => ContactsRemoved(uids)
    }
  }
}
