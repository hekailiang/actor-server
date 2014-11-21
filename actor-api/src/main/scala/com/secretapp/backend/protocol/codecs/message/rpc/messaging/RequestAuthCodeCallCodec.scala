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

object RequestAuthCodeCallCodec extends Codec[RequestAuthCodeCall] with utils.ProtobufCodec {
  def encode(r: RequestAuthCodeCall) = {
    val boxed = protobuf.RequestAuthCodeCall(r.phoneNumber, r.smsHash, r.appId, r.apiKey)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestAuthCodeCall.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestAuthCodeCall(phoneNumber, smsHash, appId, apiKey)) =>
        RequestAuthCodeCall(phoneNumber, smsHash, appId, apiKey)
    }
  }
}

