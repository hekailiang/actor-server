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

object RequestTerminateAllSessionsCodec extends Codec[RequestTerminateAllSessions] with utils.ProtobufCodec {
  def encode(r: RequestTerminateAllSessions) = {
    val boxed = protobuf.RequestTerminateAllSessions()
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestTerminateAllSessions.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestTerminateAllSessions()) =>
        RequestTerminateAllSessions()
    }
  }
}
