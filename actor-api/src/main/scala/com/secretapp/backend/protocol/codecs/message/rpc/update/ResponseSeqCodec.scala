package com.secretapp.backend.protocol.codecs.message.rpc.update

import com.google.protobuf.ByteString
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.update
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object ResponseSeqCodec extends Codec[update.ResponseSeq] with utils.ProtobufCodec {
  def encode(r: update.ResponseSeq) = {
    protoState.encode(r.state) match {
      case \/-(bytesState) =>
        val boxed = protobuf.ResponseSeq(r.seq, bytesState)
        encodeToBitVector(boxed)
      case l @ -\/(_) => l
    }
  }

  def decode(buf: BitVector) = {
    decodeProtobufEither(protobuf.ResponseAvatarChanged.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseAvatarChanged(avatar, seq, bytesState)) =>
        protoState.decodeValue(bytesState) match {
          case \/-(state) =>
            update.ResponseSeq(seq, state).right
          case l @ -\/(_) => l
        }
    }
  }
}
