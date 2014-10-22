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

object ResponseGetAuthCodec extends Codec[ResponseGetAuth] with utils.ProtobufCodec {
  def encode(r: ResponseGetAuth) = {
    val boxed = protobuf.ResponseGetAuth(r.userAuths map (_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseGetAuth.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseGetAuth(userAuths)) =>
        ResponseGetAuth(userAuths map struct.AuthItem.fromProto)
    }
  }
}
