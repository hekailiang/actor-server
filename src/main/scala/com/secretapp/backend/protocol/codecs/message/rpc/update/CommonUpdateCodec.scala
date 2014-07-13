package com.secretapp.backend.protocol.codecs.message.rpc.update

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import com.getsecretapp.{ proto => protobuf }

object CommonUpdateCodec extends Codec[CommonUpdate] with utils.ProtobufCodec {
  def encode(u : CommonUpdate) = {
    val boxed = protobuf.CommonUpdate(u.seq, u.state, u.updateId, u.update)
    encodeToBitVector(boxed)
  }

  def decode(buf : BitVector) = {
    decodeProtobuf(protobuf.CommonUpdate.parseFrom(buf.toByteArray)) {
      case Success(protobuf.CommonUpdate(seq, state, updateId, update)) =>
        CommonUpdate(seq, state, updateId, update)
    }
  }
}
