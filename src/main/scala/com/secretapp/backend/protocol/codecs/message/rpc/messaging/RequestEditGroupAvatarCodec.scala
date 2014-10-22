package com.secretapp.backend.protocol.codecs.message.rpc.messaging

import com.secretapp.backend.data.message.struct.FileLocation
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._
import scala.util.Success
import im.actor.messenger.{ api => protobuf }

object RequestEditGroupAvatarCodec extends Codec[RequestEditGroupAvatar] with utils.ProtobufCodec {
  def encode(r: RequestEditGroupAvatar) = {
    val boxed = protobuf.RequestEditGroupAvatar(r.groupId, r.accessHash, r.fileLocation.toProto)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.RequestEditGroupAvatar.parseFrom(buf.toByteArray)) {
      case Success(protobuf.RequestEditGroupAvatar(groupId, accessHash, fileLocation)) =>
        RequestEditGroupAvatar(groupId, accessHash, FileLocation.fromProto(fileLocation))
    }
  }
}
