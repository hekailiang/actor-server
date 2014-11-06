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

object RequestRemoveAllOtherAuthsCodec extends Codec[RequestRemoveAllOtherAuths] with utils.ProtobufCodec {
  def encode(r: RequestRemoveAllOtherAuths) = {
    val boxed = protobuf.RequestRemoveAllOtherAuths()
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestRemoveAllOtherAuths.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestRemoveAllOtherAuths()) =>
        RequestRemoveAllOtherAuths()
    }
  }
}
