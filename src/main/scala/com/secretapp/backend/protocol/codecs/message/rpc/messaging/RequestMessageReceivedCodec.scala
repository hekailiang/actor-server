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
import com.reactive.messenger.{ api => protobuf }

object RequestMessageReceivedCodec extends Codec[RequestMessageReceived] with utils.ProtobufCodec {
  def encode(r: RequestMessageReceived) = {
    val boxed = protobuf.RequestMessageReceived(r.uid, r.randomId, r.accessHash)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestMessageReceived.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestMessageReceived(uid, randomId, accessHash)) =>
        RequestMessageReceived(uid, randomId, accessHash)
    }
  }
}
