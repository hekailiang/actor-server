package com.secretapp.backend.protocol.codecs.message.rpc.auth

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import scodec.bits.BitVector
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object RequestSendAuthCallCodec extends Codec[RequestSendAuthCall] with utils.ProtobufCodec {
  def encode(r: RequestSendAuthCall) = {
    val boxed = protobuf.RequestSendAuthCall(r.phoneNumber, r.smsHash, r.appId, r.apiKey)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestSendAuthCall.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestSendAuthCall(phoneNumber, smsHash, appId, apiKey)) =>
        RequestSendAuthCall(phoneNumber, smsHash, appId, apiKey)
    }
  }
}
