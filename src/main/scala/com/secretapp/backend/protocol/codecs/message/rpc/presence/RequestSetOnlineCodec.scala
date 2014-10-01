package com.secretapp.backend.protocol.codecs.message.rpc.presence

import im.actor.messenger.{ api => protobuf }
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scala.util.Success
import scalaz.Scalaz._
import scodec.Codec
import scodec.bits._

object RequestSetOnlineCodec extends Codec[RequestSetOnline] with utils.ProtobufCodec {
  def encode(r: RequestSetOnline) = {
    val boxed = protobuf.RequestSetOnline(r.isOnline, r.timeout)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestSetOnline.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestSetOnline(isOnline, timeout)) =>
        RequestSetOnline(isOnline, timeout)
    }
  }
}
