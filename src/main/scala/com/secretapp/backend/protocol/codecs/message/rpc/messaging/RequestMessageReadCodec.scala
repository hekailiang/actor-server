package com.secretapp.backend.protocol.codecs.message.rpc.messaging

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import scodec.bits.BitVector
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object RequestMessageReadCodec extends Codec[RequestMessageRead] with utils.ProtobufCodec {
  def encode(r: RequestMessageRead) = {
    val boxed = protobuf.RequestMessageRead(r.uid, r.randomId, r.accessHash)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestMessageRead.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestMessageRead(uid, randomId, accessHash)) =>
        RequestMessageRead(uid, randomId, accessHash)
    }
  }
}
