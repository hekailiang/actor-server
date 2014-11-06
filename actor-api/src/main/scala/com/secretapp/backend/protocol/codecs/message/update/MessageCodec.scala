package com.secretapp.backend.protocol.codecs.message.update

import com.secretapp.backend.data.message.rpc.messaging.MessageContent
import com.secretapp.backend.data.message.struct.Peer
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object MessageCodec extends Codec[Message] with utils.ProtobufCodec {
  def encode(m: Message) = {
    val boxed = protobuf.UpdateMessage(m.peer.toProto, m.senderUid, m.date, m.randomId, m.message.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector): String \/ (BitVector, Message) = {
    decodeProtobuf(protobuf.UpdateMessage.parseFrom(buf.toByteArray)) {
      case Success(u) =>
        Message(Peer.fromProto(u.peer), u.senderUid, u.date, u.rid, MessageContent.fromProto(u.message))
    }
  }
}
