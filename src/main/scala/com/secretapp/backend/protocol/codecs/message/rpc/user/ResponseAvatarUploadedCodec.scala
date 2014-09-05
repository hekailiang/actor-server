package com.secretapp.backend.protocol.codecs.message.rpc.user

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.user._
import com.secretapp.backend.data.message.struct.Avatar
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import com.reactive.messenger.{ api => protobuf }

object ResponseAvatarUploadedCodec extends Codec[ResponseAvatarUploaded] with utils.ProtobufCodec {
  def encode(r: ResponseAvatarUploaded) = {
    val boxed = protobuf.ResponseAvatarUploaded(r.avatar.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.ResponseAvatarUploaded.parseFrom(buf.toByteArray)) {
      case Success(protobuf.ResponseAvatarUploaded(avatar)) =>
        ResponseAvatarUploaded(Avatar.fromProto(avatar))
    }
  }
}
