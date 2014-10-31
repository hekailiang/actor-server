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

object UpdateContactsRemovedCodec extends Codec[UpdateContactsRemoved] with utils.ProtobufCodec {
  def encode(u: UpdateContactsRemoved) = encodeToBitVector(u.toProto)

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateContactsRemoved.parseFrom(buf.toByteArray)) {
      case Success(protobuf.UpdateContactsRemoved(uids)) => UpdateContactsRemoved(uids)
    }
  }
}
