package com.secretapp.backend.protocol.codecs.message.update

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

object RemoveDeviceCodec extends Codec[RemoveDevice] with utils.ProtobufCodec {
  def encode(n: RemoveDevice) = {
    val boxed = protobuf.UpdateRemoveDevice(n.userId, n.keyHash)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateRemoveDevice.parseFrom(buf.toByteArray)) {
      case Success(r) => RemoveDevice(r.uid, r.keyHash)
    }
  }
}
