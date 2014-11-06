package com.secretapp.backend.protocol.codecs.message.rpc.auth

import com.secretapp.backend.crypto.ec.PublicKey
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.protocol.codecs.message.rpc.auth.RequestSignUpCodec._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object RequestSignInCodec extends Codec[RequestSignIn] with utils.ProtobufCodec {
  def encode(r: RequestSignIn) = {
    val boxed = protobuf.RequestSignIn(
      r.phoneNumber, r.smsHash, r.smsCode, r.publicKey,
      r.deviceHash, r.deviceTitle, r.appId, r.appKey
    )
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestSignIn.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestSignIn(
        phoneNumber, smsHash, smsCode, publicKey,
        deviceHash, deviceTitle, appId, appKey
      )) =>
        RequestSignIn(phoneNumber, smsHash, smsCode, publicKey, deviceHash, deviceTitle, appId, appKey)
    }
  }
}
