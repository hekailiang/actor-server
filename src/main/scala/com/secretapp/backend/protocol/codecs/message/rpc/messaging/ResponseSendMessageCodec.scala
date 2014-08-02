package com.secretapp.backend.protocol.codecs.message.rpc.messaging

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import com.getsecretapp.{ proto => protobuf }

object ResponseSendMessageCodec extends Codec[ResponseSendMessage] with utils.ProtobufCodec {
  def encode(r: ResponseSendMessage) = {
    uuid.encode(r.state) match {
      case \/-(encodedUuid) =>
        val boxed = protobuf.ResponseSendMessage(r.mid, r.seq, encodedUuid)
        encodeToBitVector(boxed)
      case l => l
    }
  }

  def decode(buf: BitVector) = {
    decodeProtobufEither(protobuf.ResponseSendMessage.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseSendMessage(mid, seq, state)) =>
        uuid.decodeValue(state) match {
          case \/-(uuidState) =>
            ResponseSendMessage(mid, seq, uuidState).right
          case l @ -\/(_) => l
        }
    }
  }
}
