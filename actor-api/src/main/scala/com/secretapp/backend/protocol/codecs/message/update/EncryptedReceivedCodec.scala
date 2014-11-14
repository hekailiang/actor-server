package com.secretapp.backend.protocol.codecs.message.update

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

object EncryptedReceivedCodec extends Codec[EncryptedReceived] with utils.ProtobufCodec {
  def encode(m: EncryptedReceived) = {
    val boxed = protobuf.UpdateEncryptedReceived(m.outPeer.toProto, m.randomId)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector): String \/ (BitVector, EncryptedReceived) = {
    decodeProtobuf(protobuf.UpdateEncryptedReceived.parseFrom(buf.toByteArray)) {
      case Success(e) => EncryptedReceived(Peer.fromProto(e.peer), e.rid)
    }
  }
}
