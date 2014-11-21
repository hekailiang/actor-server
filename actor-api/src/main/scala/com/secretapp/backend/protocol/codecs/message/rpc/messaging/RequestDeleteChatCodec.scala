package com.secretapp.backend.protocol.codecs.message.rpc.messaging

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import scodec.bits.BitVector
import scodec.Codec
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object RequestDeleteChatCodec extends Codec[RequestDeleteChat] with utils.ProtobufCodec {
  def encode(r: RequestDeleteChat) = {
    val boxed = protobuf.RequestDeleteChat(r.outPeer.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestDeleteChat.parseFrom(buf.toByteArray)) {
      case Success(r) => RequestDeleteChat(struct.OutPeer.fromProto(r.peer))
    }
  }
}
