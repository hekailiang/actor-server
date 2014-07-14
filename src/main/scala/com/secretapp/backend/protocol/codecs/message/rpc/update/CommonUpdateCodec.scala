package com.secretapp.backend.protocol.codecs.message.rpc.update

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.protocol.codecs.message.update._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import com.getsecretapp.{ proto => protobuf }

object CommonUpdateCodec extends Codec[CommonUpdate] with utils.ProtobufCodec {
  def encode(u : CommonUpdate) = {
    // TODO: kill me if I don't refactor this within two days since 15.07.14 (SA-19)
    encodeToBitVector(u.toProto)
  }

  def decode(buf : BitVector) = {
    decodeProtobuf(protobuf.CommonUpdate.parseFrom(buf.toByteArray)) {
      case Success(u@protobuf.CommonUpdate(_, _, _, _)) => CommonUpdate.fromProto(u)
    }
  }
}
