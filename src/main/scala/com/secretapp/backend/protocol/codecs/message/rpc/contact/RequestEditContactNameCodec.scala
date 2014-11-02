package com.secretapp.backend.protocol.codecs.message.rpc.contact

import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{api => protobuf}
import scodec.Codec
import scodec.bits._
import scala.util.Success

object RequestEditContactNameCodec extends Codec[RequestEditContactName] with utils.ProtobufCodec {
  def encode(r: RequestEditContactName) = {
    val boxed = protobuf.RequestEditContactName(r.uid, r.accessHash, r.name)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestEditContactName.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestEditContactName(uid, accessHash, name)) =>
        RequestEditContactName(uid, accessHash, name)
    }
  }
}
