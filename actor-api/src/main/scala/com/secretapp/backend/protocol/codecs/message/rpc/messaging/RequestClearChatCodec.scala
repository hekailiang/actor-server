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

object RequestClearChatCodec extends Codec[RequestClearChat] with utils.ProtobufCodec {
  def encode(r: RequestClearChat) = {
    val boxed = protobuf.RequestClearChat(r.outPeer.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestClearChat.parseFrom(buf.toByteArray)) {
      case Success(r) => RequestClearChat(struct.OutPeer.fromProto(r.peer))
    }
  }
}
