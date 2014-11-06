package com.secretapp.backend.protocol.codecs.message.rpc.history

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.history._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import scodec.bits.BitVector
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object RequestLoadDialogsCodec extends Codec[RequestLoadDialogs] with utils.ProtobufCodec {
  def encode(r: RequestLoadDialogs) = {
    val boxed = protobuf.RequestLoadDialogs(r.startDate, r.limit)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestLoadDialogs.parseFrom(buf.toByteArray)) {
      case Success(r: protobuf.RequestLoadDialogs) => RequestLoadDialogs(r.startDate, r.limit)
    }
  }
}
