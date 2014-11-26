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

object RemovedDeviceCodec extends Codec[RemovedDevice] with utils.ProtobufCodec {
  def encode(n: RemovedDevice) = {
    val boxed = protobuf.UpdateRemovedDevice(n.userId, n.keyHash)
    encodeToBitVector(boxed)
  }

  def decode(buf: BitVector) = {
    decodeProtobuf(protobuf.UpdateRemovedDevice.parseFrom(buf.toByteArray)) {
      case Success(r) => RemovedDevice(r.uid, r.keyHash)
    }
  }
}
