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

object ResponseAuthCodeCodec extends Codec[ResponseAuthCode] with utils.ProtobufCodec {
  def encode(r: ResponseAuthCode) = {
    val boxed = protobuf.ResponseAuthCode(r.smsHash, r.isRegistered)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseAuthCode.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseAuthCode(smsHash, isRegistered)) =>
        ResponseAuthCode(smsHash, isRegistered)
    }
  }
}
