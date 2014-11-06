package com.secretapp.backend.protocol.codecs.message.rpc.auth

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.crypto.ec.PublicKey
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object RequestSignUpCodec extends Codec[RequestSignUp] with utils.ProtobufCodec {
  def encode(r: RequestSignUp) = {
    val boxed = protobuf.RequestSignUp(
      r.phoneNumber, r.smsHash, r.smsCode, r.name, r.publicKey,
      r.deviceHash, r.deviceTitle, r.appId, r.appKey
    )
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestSignUp.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestSignUp(
        phoneNumber, smsHash, smsCode, name, publicKey,
        deviceHash, deviceTitle, appId, appKey
      )) =>
        RequestSignUp(phoneNumber, smsHash, smsCode, name, publicKey, deviceHash, deviceTitle, appId, appKey)
    }
  }
}
