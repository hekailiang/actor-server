package com.secretapp.backend.protocol.codecs.message.rpc.update

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.update
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import com.getsecretapp.{ proto => protobuf }

object StateCodec extends Codec[update.State] with utils.ProtobufCodec {
  def encode(s : update.State) = {
    val boxed = protobuf.State(s.seq, s.state)
    encodeToBitVector(boxed)
  }

  def decode(buf : BitVector) = {
    decodeProtobuf(protobuf.State.parseFrom(buf.toByteArray)) {
      case Success(protobuf.State(seq, state)) => update.State(seq, state)
    }
  }
}
