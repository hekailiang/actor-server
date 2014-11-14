package com.secretapp.backend.protocol.codecs.message.rpc.contact

import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{api => protobuf}
import scodec.Codec
import scodec.bits._
import scala.util.Success

object RequestEditUserLocalNameCodec extends Codec[RequestEditUserLocalName] with utils.ProtobufCodec {
  def encode(r: RequestEditUserLocalName) = {
    val boxed = protobuf.RequestEditUserLocalName(r.userId, r.accessHash, r.name)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestEditUserLocalName.parseFrom(buf.toByteArray)) {
      case Success(r) => RequestEditUserLocalName(r.uid, r.accessHash, r.name)
    }
  }
}
