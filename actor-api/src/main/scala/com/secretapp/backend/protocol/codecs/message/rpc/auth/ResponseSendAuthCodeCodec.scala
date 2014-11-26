package com.secretapp.backend.protocol.codecs.message.rpc.auth

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object ResponseSendAuthCodeCodec extends Codec[ResponseSendAuthCode] with utils.ProtobufCodec {
  def encode(r: ResponseSendAuthCode) = {
    val boxed = protobuf.ResponseSendAuthCode(r.smsHash, r.isRegistered)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseSendAuthCode.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseSendAuthCode(smsHash, isRegistered)) =>
        ResponseSendAuthCode(smsHash, isRegistered)
    }
  }
}
