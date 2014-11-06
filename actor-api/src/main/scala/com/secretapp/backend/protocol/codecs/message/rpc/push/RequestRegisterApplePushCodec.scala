package com.secretapp.backend.protocol.codecs.message.rpc.push

import com.secretapp.backend.data.message.rpc.push.RequestRegisterApplePush
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{ api => protobuf }
import scala.util.Success
import scodec.Codec
import scodec.bits._
import scodec.codecs._

object RequestRegisterApplePushCodec extends Codec[RequestRegisterApplePush] with utils.ProtobufCodec {
  def encode(r: RequestRegisterApplePush) = {
    val boxed = protobuf.RequestRegisterApplePush(r.apnsKey, r.token)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestRegisterApplePush.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestRegisterApplePush(apnsKey, token)) =>
        RequestRegisterApplePush(apnsKey, token)
    }
  }
}
