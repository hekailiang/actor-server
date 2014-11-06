package com.secretapp.backend.protocol.codecs.message.rpc.messaging

import com.google.protobuf.ByteString
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

object ResponseCreateGroupCodec extends Codec[ResponseCreateGroup] with utils.ProtobufCodec {
  def encode(r: ResponseCreateGroup) = {
    val state = r.state match {
      case Some(realState) =>
        uuid.encode(realState).toOption.get
      case None => BitVector.empty
    }
    val boxed = protobuf.ResponseCreateGroup(r.groupId, r.accessHash, r.seq, state)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobufEither(protobuf.ResponseCreateGroup.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseCreateGroup(groupId, accessHash, seq, state)) =>
        state match {
          case ByteString.EMPTY =>
            ResponseCreateGroup(groupId, accessHash, seq, None).right
          case _ =>
            uuid.decodeValue(state) match {
              case \/-(uuidState) =>
                ResponseCreateGroup(groupId, accessHash, seq, Some(uuidState)).right
              case l @ -\/(_) =>
                l
            }
        }

    }
  }
}
