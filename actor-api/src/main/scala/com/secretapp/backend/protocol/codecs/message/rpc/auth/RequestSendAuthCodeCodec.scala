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
import im.actor.messenger.{ api => protobuf }

object RequestSendAuthCodeCodec extends Codec[RequestSendAuthCode] with utils.ProtobufCodec {
  def encode(r: RequestSendAuthCode) = {
    val boxed = protobuf.RequestSendAuthCode(r.phoneNumber, r.appId, r.apiKey)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestSendAuthCode.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestSendAuthCode(phoneNumber, appId, apiKey)) =>
        RequestSendAuthCode(phoneNumber, appId, apiKey)
    }
  }
}
