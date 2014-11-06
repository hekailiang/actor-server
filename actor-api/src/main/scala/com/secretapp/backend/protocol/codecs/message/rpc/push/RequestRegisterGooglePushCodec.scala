package com.secretapp.backend.protocol.codecs.message.rpc.push

import com.secretapp.backend.data.message.rpc.push.RequestRegisterGooglePush
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{ api => protobuf }
import scala.util.Success
import scodec.Codec
import scodec.bits._
import scodec.codecs._

object RequestRegisterGooglePushCodec extends Codec[RequestRegisterGooglePush] with utils.ProtobufCodec {
  def encode(r: RequestRegisterGooglePush) = {
    val boxed = protobuf.RequestRegisterGooglePush(r.projectId, r.token)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestRegisterGooglePush.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestRegisterGooglePush(projectId, token)) =>
        RequestRegisterGooglePush(projectId, token)
    }
  }
}
