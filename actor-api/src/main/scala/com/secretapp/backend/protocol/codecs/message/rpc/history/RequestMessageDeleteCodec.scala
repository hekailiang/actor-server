package com.secretapp.backend.protocol.codecs.message.rpc.history

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.history._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import scodec.bits.BitVector
import scodec.Codec
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object RequestMessageDeleteCodec extends Codec[RequestMessageDelete] with utils.ProtobufCodec {
  def encode(r: RequestMessageDelete) = {
    val boxed = protobuf.RequestMessageDelete(r.outPeer.toProto, r.randomIds)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestMessageDelete.parseFrom(buf.toByteArray)) {
      case Success(r) => RequestMessageDelete(struct.OutPeer.fromProto(r.peer), r.rid)
    }
  }
}
