package com.secretapp.backend.protocol.codecs.message.rpc.auth

import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{ api => protobuf }
import scala.util.Success
import scalaz._
import Scalaz._
import scodec.Codec
import scodec.bits._
import scodec.codecs._

object RequestLogoutCodec extends Codec[RequestLogout] with utils.ProtobufCodec {
  def encode(r: RequestLogout) = {
    val boxed = protobuf.RequestLogout()
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestLogout.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestLogout()) =>
        RequestLogout()
    }
  }
}
