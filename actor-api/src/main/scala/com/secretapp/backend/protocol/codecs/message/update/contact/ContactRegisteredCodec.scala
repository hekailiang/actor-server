package com.secretapp.backend.protocol.codecs.message.update.contact

import com.secretapp.backend.data.message.struct.User
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.update.contact._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object ContactRegisteredCodec extends Codec[ContactRegistered] with utils.ProtobufCodec {
  def encode(n: ContactRegistered) = {
    val boxed = protobuf.UpdateContactRegistered(n.userId, n.isSilent, n.date)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateContactRegistered.parseFrom(buf.toByteArray)) {
      case Success(r) => ContactRegistered(r.uid, r.isSilent, r.date)
    }
  }
}
