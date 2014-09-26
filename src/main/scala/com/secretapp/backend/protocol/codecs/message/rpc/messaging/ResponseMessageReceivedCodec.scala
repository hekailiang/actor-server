package com.secretapp.backend.protocol.codecs.message.rpc.messaging

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import com.reactive.messenger.{ api => protobuf }

object ResponseMessageReceivedCodec extends Codec[ResponseMessageReceived] with utils.ProtobufCodec {
  def encode(r: ResponseMessageReceived) = {
    val boxed = protobuf.ResponseMessageReceived(r.seq)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseMessageReceived.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseMessageReceived(seq)) =>
        ResponseMessageReceived(seq)
    }
  }
}
