package com.secretapp.backend.protocol.codecs.message.rpc.messaging

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import scodec.bits.BitVector
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object RequestSendMessageCodec extends Codec[RequestSendMessage] with utils.ProtobufCodec {
  def encode(r: RequestSendMessage) = {
    val boxed = protobuf.RequestSendMessage(r.peer.toProto, r.randomId, r.message.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestSendMessage.parseFrom(buf.toByteArray)) {
      case Success(r) =>
        RequestSendMessage(struct.OutPeer.fromProto(r.peer), r.rid, MessageContent.fromProto(r.message))
    }
  }
}
