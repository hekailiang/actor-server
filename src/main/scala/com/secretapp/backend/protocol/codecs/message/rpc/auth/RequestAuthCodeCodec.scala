package com.secretapp.backend.protocol.codecs.message.rpc.auth

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import com.reactive.messenger.{ api => protobuf }

object RequestAuthCodeCodec extends Codec[RequestAuthCode] with utils.ProtobufCodec {
  def encode(r: RequestAuthCode) = {
    val boxed = protobuf.RequestAuthCode(r.phoneNumber, r.appId, r.apiKey)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestAuthCode.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestAuthCode(phoneNumber, appId, apiKey)) =>
        RequestAuthCode(phoneNumber, appId, apiKey)
    }
  }
}
