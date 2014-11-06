package com.secretapp.backend.protocol.codecs.message.rpc.contact

import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{api => protobuf}
import scodec.Codec
import scodec.bits._
import scala.util.Success

object RequestRemoveContactCodec extends Codec[RequestRemoveContact] with utils.ProtobufCodec {
  def encode(r: RequestRemoveContact) = {
    val boxed = protobuf.RequestRemoveContact(r.uid, r.accessHash)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestRemoveContact.parseFrom(buf.toByteArray)) {
      case Success(r) => RequestRemoveContact(r.uid, r.accessHash)
    }
  }
}
