package com.secretapp.backend.protocol.codecs.message.rpc.messaging

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.history._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import com.secretapp.backend.data.message.struct
import scodec.bits.BitVector
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object ResponseDialogsCodec extends Codec[ResponseDialogs] with utils.ProtobufCodec {
  def encode(r: ResponseDialogs) = {
    val boxed = protobuf.ResponseDialogs(r.groups.map(_.toProto), r.users.map(_.toProto), r.dialogs.map(_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseDialogs.parseFrom(buf.toByteArray)) {
      case Success(r: protobuf.ResponseDialogs) =>
        ResponseDialogs(r.groups.map(struct.Group.fromProto),
          r.users.map(struct.User.fromProto), r.dialogs.map(Dialog.fromProto))
    }
  }
}
