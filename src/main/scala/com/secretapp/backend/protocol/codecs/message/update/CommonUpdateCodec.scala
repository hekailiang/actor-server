package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.{ Try, Success, Failure }

object CommonUpdateCodec extends Codec[CommonUpdate] {
  def encode(u : CommonUpdate) = {
    val boxed = com.getsecretapp.proto.CommonUpdate(u.seq, u.state, u.updateId, u.update)
    encodeToBitVector(boxed)
  }

  def decode(buf : BitVector) = {
    Try(com.getsecretapp.proto.CommonUpdate.parseFrom(buf.toByteArray)) match {
      case Success(com.getsecretapp.proto.CommonUpdate(seq, state, updateId, update)) =>
        (BitVector.empty, CommonUpdate(seq, state, updateId, update)).right
      case Failure(e) => s"parse error: ${e.getMessage}".left
    }
  }
}
