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

object RequestCreateChatCodec extends Codec[RequestCreateChat] with utils.ProtobufCodec {
  def encode(r: RequestCreateChat) = {
    val boxed = protobuf.RequestCreateChat(r.randomId, r.title, r.keyHash, r.publicKey, r.invites map (_.toProto))
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestCreateChat.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestCreateChat(randomId, title, keyHash, publicKey, invites)) =>
        RequestCreateChat(randomId, title, keyHash, publicKey, invites map InviteUser.fromProto)
    }
  }
}
